package com.kanade.backend.assembly;

import com.kanade.backend.assembly.constraint.ConstraintValidator;
import com.kanade.backend.assembly.model.AssemblyConstraint;
import com.kanade.backend.assembly.model.PaperCandidate;
import com.kanade.backend.assembly.model.QuestionScore;
import com.kanade.backend.assembly.scorer.CompositeScorer;
import com.kanade.backend.assembly.scorer.FitnessCalculator;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GeneticAlgorithm {

    private static final int POPULATION_SIZE = 30;
    private static final int MAX_GENERATIONS = 20;
    private static final double MUTATION_RATE = 0.03;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int TOURNAMENT_SIZE = 3;
    private static final int ELITE_COUNT = 2;
    private static final double EARLY_STOP_FITNESS = 0.95;
    private static final int EARLY_STOP_GENERATIONS = 8;

    private final CompositeScorer scorer;
    private final FitnessCalculator fitnessCalculator;
    private final AssemblyConstraint constraints;
    private final Random random = new Random();

    public GeneticAlgorithm(CompositeScorer scorer, FitnessCalculator fitnessCalculator, AssemblyConstraint constraints) {
        this.scorer = scorer;
        this.fitnessCalculator = fitnessCalculator;
        this.constraints = constraints;
    }

    /**
     * 遗传算法组卷
     * 1. 贪心初始化种群（30个体）
     * 2. 迭代最多20代：适应度计算 → 选择 → 交叉 → 变异 → 精英保留
     * 3. 早停：最优适应度 > 0.95 或 8代无提升
     */
    public GeneticResult assemble(List<QuestionScore> candidatePool) {
        long startTime = System.currentTimeMillis();

        // 1. 候选池缩小到200题以内
        List<QuestionScore> reducedPool = candidatePool.stream()
                .sorted(Comparator.comparingDouble(QuestionScore::getCompositeScore).reversed())
                .limit(200)
                .collect(Collectors.toList());

        // 2. 贪心初始化种群
        GreedyAlgorithm greedyInit = new GreedyAlgorithm(scorer, constraints);
        List<List<QuestionScore>> rawPopulation = greedyInit.generateInitialPopulation(reducedPool, POPULATION_SIZE);

        // 3. 转换为 PaperCandidate
        List<PaperCandidate> population = rawPopulation.stream()
                .map(this::toCandidate)
                .filter(c -> c.size() > 0)
                .collect(Collectors.toList());

        // 补充随机个体到种群规模
        while (population.size() < POPULATION_SIZE) {
            List<QuestionScore> randomPaper = generateRandomPaper(reducedPool);
            population.add(toCandidate(randomPaper));
        }

        // 4. 计算初始适应度
        for (PaperCandidate c : population) {
            c.setFitness(fitnessCalculator.calculate(c));
        }

        // 5. 遗传迭代
        PaperCandidate bestEver = population.stream()
                .max(Comparator.comparingDouble(PaperCandidate::getFitness))
                .orElse(population.get(0)).copy();
        int noImprovementCount = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            // 按适应度降序
            population.sort(Comparator.comparingDouble(PaperCandidate::getFitness).reversed());

            PaperCandidate currentBest = population.get(0);
            if (currentBest.getFitness() > bestEver.getFitness()) {
                bestEver = currentBest.copy();
                noImprovementCount = 0;
            } else {
                noImprovementCount++;
            }

            // 早停
            if (currentBest.getFitness() >= EARLY_STOP_FITNESS || noImprovementCount >= EARLY_STOP_GENERATIONS) {
                log.info("[遗传] 早停于第{}代，最优适应度={}", gen, currentBest.getFitness());
                break;
            }

            // 新一代
            List<PaperCandidate> newPopulation = new ArrayList<>();

            // 精英保留
            for (int i = 0; i < Math.min(ELITE_COUNT, population.size()); i++) {
                newPopulation.add(population.get(i).copy());
            }

            // 交叉变异填充
            while (newPopulation.size() < POPULATION_SIZE) {
                PaperCandidate parent1 = tournamentSelect(population);
                PaperCandidate parent2 = tournamentSelect(population);

                PaperCandidate child;
                if (random.nextDouble() < CROSSOVER_RATE) {
                    child = crossover(parent1, parent2);
                } else {
                    child = parent1.copy();
                }

                if (random.nextDouble() < MUTATION_RATE) {
                    mutate(child, reducedPool);
                }

                child.setFitness(fitnessCalculator.calculate(child));
                newPopulation.add(child);
            }

            population = newPopulation;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[遗传] 组卷完成，耗时: {}ms, 适应度: {}, 题目数: {}",
                elapsed, bestEver.getFitness(), bestEver.size());

        return new GeneticResult(bestEver, elapsed);
    }

    private PaperCandidate toCandidate(List<QuestionScore> questions) {
        PaperCandidate c = new PaperCandidate();
        c.setQuestionScores(new ArrayList<>(questions));
        c.setQuestionIds(questions.stream().map(QuestionScore::getQuestionId).collect(Collectors.toList()));
        return c;
    }

    private List<QuestionScore> generateRandomPaper(List<QuestionScore> pool) {
        List<QuestionScore> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        List<QuestionScore> selected = new ArrayList<>();
        ConstraintValidator validator = new ConstraintValidator(constraints);
        for (QuestionScore qs : shuffled) {
            if (validator.canAdd(selected, qs)) {
                selected.add(qs);
            }
        }
        return selected;
    }

    private PaperCandidate tournamentSelect(List<PaperCandidate> population) {
        PaperCandidate best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            PaperCandidate candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.getFitness() > best.getFitness()) {
                best = candidate;
            }
        }
        return best != null ? best.copy() : population.get(0).copy();
    }

    /**
     * 单点交叉：随机选择交叉点，交换父代该点前后的题目
     */
    private PaperCandidate crossover(PaperCandidate parent1, PaperCandidate parent2) {
        PaperCandidate child = parent1.copy();
        if (parent2.size() == 0) return child;

        int size = Math.min(parent1.size(), parent2.size());
        if (size < 2) return child;

        int crossoverPoint = random.nextInt(size - 1) + 1;

        // 取parent1的前半 + parent2的后半，去重
        Set<Long> usedIds = new HashSet<>();
        List<QuestionScore> newQuestions = new ArrayList<>();

        for (int i = 0; i < crossoverPoint && i < parent1.size(); i++) {
            newQuestions.add(parent1.getQuestionScores().get(i));
            usedIds.add(parent1.getQuestionScores().get(i).getQuestionId());
        }
        for (int i = crossoverPoint; i < parent2.size(); i++) {
            QuestionScore qs = parent2.getQuestionScores().get(i);
            if (!usedIds.contains(qs.getQuestionId())) {
                newQuestions.add(qs);
                usedIds.add(qs.getQuestionId());
            }
        }

        child.setQuestionScores(newQuestions);
        child.setQuestionIds(newQuestions.stream().map(QuestionScore::getQuestionId).collect(Collectors.toList()));
        return child;
    }

    /**
     * 变异：随机替换1-2道题
     */
    private void mutate(PaperCandidate candidate, List<QuestionScore> pool) {
        if (candidate.size() == 0 || pool.isEmpty()) return;

        int mutateCount = random.nextDouble() < 0.5 ? 1 : 2;
        Set<Long> currentIds = new HashSet<>(candidate.getQuestionIds());

        for (int i = 0; i < mutateCount; i++) {
            if (candidate.size() == 0) break;

            // 随机移除一道题
            int removeIdx = random.nextInt(candidate.size());
            QuestionScore removed = candidate.getQuestionScores().remove(removeIdx);
            candidate.getQuestionIds().remove(removeIdx);
            currentIds.remove(removed.getQuestionId());

            // 随机添加一道新题
            List<QuestionScore> available = pool.stream()
                    .filter(q -> !currentIds.contains(q.getQuestionId()))
                    .collect(Collectors.toList());
            if (!available.isEmpty()) {
                QuestionScore replacement = available.get(random.nextInt(available.size()));
                candidate.getQuestionScores().add(replacement);
                candidate.getQuestionIds().add(replacement.getQuestionId());
                currentIds.add(replacement.getQuestionId());
            }
        }
    }

    public record GeneticResult(PaperCandidate bestCandidate, long elapsedMs) {}
}
