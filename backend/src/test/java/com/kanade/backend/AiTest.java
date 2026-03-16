package com.kanade.backend;

import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.model.LabelResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AiTest {

    @Resource
    AiService aiService;
    @Test
    void generateHtmlCode() {
        LabelResult string = aiService.generateQuestionLabel("抛物线 y=2(x−3)2+1 的顶点坐标是（）A. (3,1)  B. (-3,1)  C. (3,-1)  D. (-3,-1)");
        System.out.println(string);
        Assertions.assertNotNull(string);
    }

    @Test
    void generate() {
        String string = "41.大模型赋能，生成式人工智能正在引发新一轮智能化浪潮。得益于拥有庞大的数据、参数以及较好的学习能力，大模型增强了人工智能的通用性。从与人顺畅聊天到写合同、剧本，从检测程序安全漏洞到辅助创作游戏甚至电影，生成式人工智能本领加速进化。随着技术迭代，更高效、更“聪明”的大模型将渗透到越来越多的领域，有望成为人工智能技术及应用的新基座，变成人们生产生活的基础性工具，进而带来经济社会发展和产业的深刻变革。人工智能大模型强大的创新潜能，使其成为全球竞争的焦点之一。\n" +
                "\n" +
                "这段文字意在说明:\n" +
                "\n" +
                "A、人工智能在大模型赋能下正在快速发展\n" +
                "\n" +
                "B、加快发展新一代人工智能势在必行\n" +
                "\n" +
                "C、生成式人工智能的创新潜能亟待挖掘\n" +
                "\n" +
                "D、生成式人工智能已成为全球研究的焦点";
        LabelResult sss = aiService.generateQuestionLabel(string);
        System.out.println(sss);
        Assertions.assertNotNull(sss);
    }

    @Test
    void generate1() {
        String string = "47.①据有关机构估算，每年损失浪费的食物超过22.7%，约9200亿斤，若能挽回一半的损失，就够1.9亿人吃一年\n" +
                "\n" +
                "②食物节约减损既可有效减轻供给压力，也可减少资源使用，善莫大焉\n" +
                "\n" +
                "③我国居民食用油和“红肉”人均消费量，分别超过膳食指南推荐标准约1倍和2倍\n" +
                "\n" +
                "④当前，食物采收、储运、加工、销售、消费每个环节都有“跑冒滴漏”，情况还相当严重\n" +
                "\n" +
                "⑤要树立节约减损就是增产的理念，推进全链条节约减损，健全常态化、长效化工作机制，每个环节都要有具体抓手，越是损失浪费严重的环节越要抓得实\n" +
                "\n" +
                "⑥消费环节大有文章可做，不仅要制止“舌尖上的浪费”，深入开展“光盘行动”，还要提倡健康饮食\n" +
                "\n" +
                "将以上6个句子重新排列，语序正确的一项是：\n" +
                "\n" +
                "A、①⑥③②④⑤\n" +
                "\n" +
                "B、①③④⑤②⑥\n" +
                "\n" +
                "C、④①⑥③②⑤\n" +
                "\n" +
                "D、④②⑤③①⑥";
        LabelResult sss = aiService.generateQuestionLabel(string);
        System.out.println(sss);
        Assertions.assertNotNull(sss);
    }

    @Test
    void generate2() {
        String string = "86.慢病也称为慢性非传染性疾病，是指长期的、不能自愈的、也几乎不能被治愈的疾病。慢病自我管理是指慢病患者主动监测自己的病情，以积极态度及行动，改善健康和情绪，通过对疾病的认识，学习与疾病长期共存。\n" +
                "\n" +
                "根据上述定义，下列属于慢病自我管理的是：\n" +
                "\n" +
                "A、老张在患了流感后多方查阅相关信息，每天监测血氧、心率、血压等指标，不做剧烈运动，避免着凉\n" +
                "\n" +
                "B、老王因慢性肾功能衰竭需定期在医院进行透析治疗，医院为其建立了慢病管理档案，追踪监测肾功能等各项指标\n" +
                "\n" +
                "C、老赵的父母都因胃癌去世，老赵深知该疾病的严重后果，每年全面体检一次，定期进行胃肠镜检查，不熬夜不喝酒，规律作息\n" +
                "\n" +
                "D、患II型糖尿病的老李，平日生活格外谨慎，含糖量高的食物一概不碰，每天按时服药，定期监测血糖水平";
        LabelResult sss = aiService.generateQuestionLabel(string);
        System.out.println(sss);
        Assertions.assertNotNull(sss);
    }
    @Test
    void generate3() {
        String string = "110.墨西哥丽脂鲤是一类洞穴鱼，生活在寒冷漆黑的洞穴中，它们的身体不产生色素，因此呈现白化状态。与其他鱼类相比，墨西哥丽脂鲤的某一基因发生突变，使其难以合成酪氨酸，因此抑制了黑色素的形成。研究人员认为这一基因改变并不是生物进化中的偶然事件，而是鱼类为了生存所做出的适应性策略。\n" +
                "\n" +
                "以下除哪项外，均能支持研究人员的观点？\n" +
                "\n" +
                "A、酪氨酸是合成多巴胺等激素的前体，许多动物在应对生存压力时都会分泌多巴胺类物质\n" +
                "\n" +
                "B、与墨西哥丽脂鲤类似，一些生存在阴暗寒冷环境中的海底生物也会出现身体白化现象\n" +
                "\n" +
                "C、这一基因改变促使墨西哥丽脂鲤血液中的红细胞体积更大，血红蛋白容量更高，更能应对缺氧环境\n" +
                "\n" +
                "D、形成黑色素需要耗费能量，而墨西哥丽脂鲤长期处在能量匮乏的环境中，减少酪氨酸的合成有助于自身能量的储备";
        LabelResult sss = aiService.generateQuestionLabel(string);
        System.out.println(sss);
        Assertions.assertNotNull(sss);
    }
    @Test
    void generate4() {
        String string = "网络安全态势感知在（3）的基础上，进行数据整合，特征提取等，应用一系列态势评估算法，生成网络的整体态势情况。\n" +
                "\n" +
                "A、安全应用软件\n" +
                "\n" +
                "B、安全基础设施\n" +
                "\n" +
                "C、安全网络环境\n" +
                "\n" +
                "D、安全大数据";
        LabelResult sss = aiService.generateQuestionLabel(string);
        System.out.println(sss);
        Assertions.assertNotNull(sss);
    }
   @Test
    void generate5() {
        String string = "4、（4）是指一个操作系统中多个程序同时并行运行，而（4）则可以同时运行多个操作系统，而且每一个操作系统中都有多个程序运行，（4）只是单CPU模拟双CPU来平衡程序运行性能，这两个模拟出来的 CPU 是不能分离的，只能协同工作。A、虚拟化技术 多任务 超线程技术  B、超线程技术 虚拟化技术 多任务C、虚拟化技术 超线程技术 多任务  D、多任务 虚拟化技术 超线程技术答案：D解析:《信息系统项目管理师教程》第四版P52页。多任务是指在一个操作系统中多个程序同时并行运行，而在虚拟化技术中，则可同时运行多个操作系统，而且每一个操作系统中都有多个程序运行，每一个操作系统都运在一个虚拟的 CPU 或者虚拟主机上。超线程技术只是单 CPU 模拟双 CPU 来平衡程序运行性能，这两个模拟出来的 CPU 是不能分离的，只能协同工作。\n" +
                "\n" +
                "作者：知乎用户m2TTZ0\n" +
                "链接：https://zhuanlan.zhihu.com/p/636000069\n" +
                "来源：知乎\n" +
                "著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。";
       LabelResult sss = aiService.generateQuestionLabel(string);
        System.out.println(sss);
        Assertions.assertNotNull(sss);
    }



}
