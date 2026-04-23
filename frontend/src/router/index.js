import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

import LoginView          from '../features/auth/LoginView.vue'
import WARView            from '../features/war/WARView.vue'
import PeerEvalView       from '../features/peer-eval/PeerEvalView.vue'
import MyReportView       from '../features/report/MyReportView.vue'
import SectionReportView           from '../features/instructor/SectionReportView.vue'
import TeamWARView                 from '../features/instructor/TeamWARView.vue'
import StudentPeerEvalReportView   from '../features/instructor/StudentPeerEvalReportView.vue'
import StudentWARReportView        from '../features/instructor/StudentWARReportView.vue'
import SectionsView       from '../features/admin/SectionsView.vue'
import TeamsView          from '../features/admin/TeamsView.vue'
import TeamDetailView     from '../features/admin/TeamDetailView.vue'
import RubricsView        from '../features/admin/RubricsView.vue'
import InvitationsView    from '../features/admin/InvitationsView.vue'

const routes = [
  { path: '/',                  redirect: '/login' },
  { path: '/login',             component: LoginView,  meta: { public: true } },
  { path: '/register/:token',   component: () => import('../features/auth/RegisterView.vue'), meta: { public: true } },

  // Student
  { path: '/war',               component: WARView,           meta: { role: 'student' } },
  { path: '/peer-eval',         component: PeerEvalView,      meta: { role: 'student' } },
  { path: '/my-report',         component: MyReportView,      meta: { role: 'student' } },

  // Instructor
  { path: '/section-report',                          component: SectionReportView,         meta: { role: 'instructor' } },
  { path: '/team-war',                                component: TeamWARView,               meta: { role: 'instructor' } },
  { path: '/instructor/students/:id/peer-eval-report', component: StudentPeerEvalReportView, meta: { role: 'instructor' } },
  { path: '/instructor/students/:id/war-report',       component: StudentWARReportView,      meta: { role: 'instructor' } },

  // Admin
  { path: '/admin/sections',    component: SectionsView,      meta: { role: 'admin' } },
  { path: '/admin/teams',       component: TeamsView,         meta: { role: 'admin' } },
  { path: '/admin/teams/:id',   component: TeamDetailView,    meta: { role: 'admin' } },
  { path: '/admin/rubrics',     component: RubricsView,       meta: { role: 'admin' } },
  { path: '/admin/invitations', component: InvitationsView,   meta: { role: 'admin' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.user) return '/login'
  if (to.meta.role && auth.user?.role !== to.meta.role) {
    return defaultRoute(auth.user?.role)
  }
})

function defaultRoute(role) {
  if (role === 'student')    return '/war'
  if (role === 'instructor') return '/section-report'
  if (role === 'admin')      return '/admin/sections'
  return '/login'
}

export { router, defaultRoute }
