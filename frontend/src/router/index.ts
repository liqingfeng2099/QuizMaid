import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/user/login",
    component: () => import("@/views/UserLogin.vue")
  },
  {
    path: "/user/register",
    component: () => import("@/views/UserRegister.vue")
  },
  {
    path: "/oauth/github/callback",
    component: () => import("@/views/GithubCallback.vue")
  },
  {
    path: "/",
    redirect: "/home"
  },
  {
    path: "/home",
    component: () => import("@/views/HomeView.vue")
  },
  {
    path: "/question",
    component: () => import("@/views/QuestionView.vue")
  },
  {
    path: "/paper",
    component: () => import("@/views/PaperView.vue")
  },
  {
    path: "/paper/assembly",
    component: () => import("@/views/AssemblyView.vue")
  },
  {
    path: "/exam",
    component: () => import("@/views/ExamView.vue")
  },
  {
    path: "/error-book",
    component: () => import("@/views/ErrorBookView.vue")
  },
  {
    path: "/profile",
    component: () => import("@/views/ProfileView.vue")
  },
  {
    path: "/system",
    component: () => import("@/views/SystemView.vue")
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router
