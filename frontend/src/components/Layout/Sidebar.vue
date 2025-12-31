<template>
  <aside class="sidebar">
    <nav class="sidebar-nav">
      <router-link 
        to="/repositories" 
        class="nav-item"
        :class="{ active: $route.name === 'Repositories' || $route.name === 'RepositoryDetail' }"
      >
        repositories
      </router-link>
      <router-link 
        to="/exam" 
        class="nav-item"
        :class="{ active: $route.name === 'Exam' }"
      >
        exam
      </router-link>
      <router-link 
        to="/review" 
        class="nav-item"
        :class="{ active: $route.name === 'Review' }"
      >
        review
      </router-link>
      <router-link 
        to="/group" 
        class="nav-item"
        :class="{ active: $route.name === 'Group' }"
      >
        group
      </router-link>
      <router-link 
        to="/profile" 
        class="nav-item"
        :class="{ active: $route.name === 'Profile' }"
      >
        profile
      </router-link>
      <router-link 
        to="/community" 
        class="nav-item"
        :class="{ active: $route.name === 'Community' }"
      >
        community
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
</script>

<style scoped>
.sidebar {
  width: 200px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F0F7FF 100%);
  border-right: 1px solid rgba(187, 222, 251, 0.5);
  padding: 16px 0;
  box-shadow: 2px 0 8px rgba(33, 150, 243, 0.08);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  padding: 12px 24px;
  text-decoration: none;
  color: #546E7A;
  font-size: 14px;
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
  position: relative;
}

.nav-item:hover {
  background: linear-gradient(to right, rgba(227, 242, 253, 0.6) 0%, rgba(187, 222, 251, 0.3) 100%);
  color: #1976d2;
  transform: translateX(2px);
}

.nav-item.active {
  background: linear-gradient(to right, #E3F2FD 0%, rgba(187, 222, 251, 0.4) 100%);
  color: #1565C0;
  border-left-color: #2196F3;
  font-weight: 600;
  box-shadow: inset 0 0 10px rgba(33, 150, 243, 0.1);
}
</style>
