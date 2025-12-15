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
    </nav>
  </aside>
</template>

<script setup>
</script>

<style scoped>
.sidebar {
  width: 200px;
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  padding: 16px 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  padding: 12px 24px;
  text-decoration: none;
  color: #666;
  font-size: 14px;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background-color: #f5f5f5;
  color: #333;
}

.nav-item.active {
  background-color: #e3f2fd;
  color: #1976d2;
  border-left-color: #1976d2;
  font-weight: 500;
}
</style>
