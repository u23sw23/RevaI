import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout/Layout.vue'
import Repositories from '../views/Repositories.vue'
import RepositoryDetail from '../views/RepositoryDetail.vue'
import Exam from '../views/Exam.vue'
import Group from '../views/Group.vue'
import Profile from '../views/Profile.vue'
import Community from '../views/Community.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        redirect: '/repositories'
      },
      {
        path: 'repositories',
        name: 'Repositories',
        component: Repositories
      },
      {
        path: 'repositories/:id',
        name: 'RepositoryDetail',
        component: RepositoryDetail,
        props: true
      },
      {
        path: 'exam',
        name: 'Exam',
        component: Exam
      },
      {
        path: 'group',
        name: 'Group',
        component: Group
      },
      {
        path: 'profile',
        name: 'Profile',
        component: Profile
      },
      {
        path: 'community',
        name: 'Community',
        component: Community
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
