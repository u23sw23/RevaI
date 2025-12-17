// 模拟数据：科目 -> 笔记 -> 内容
export const subjects = [
  {
    id: 'math',
    name: '数学',
    description: '涵盖代数、几何、概率与统计的核心知识点',
    notes: [
      {
        id: 'math-note-1',
        title: '函数与极限',
        content: `1. 函数定义域与值域
2. 极限的几何意义与求解方法
3. 常见极限公式与等价无穷小
4. 洛必达法则的使用条件与示例
5. 连续与间断点判定`,
        updatedAt: '2024-12-10'
      },
      {
        id: 'math-note-2',
        title: '导数与微分',
        content: `1. 导数的四则运算法则
2. 常见复合函数求导技巧
3. 隐函数与参数方程求导
4. 极值、单调性与凹凸性判定
5. 泰勒展开与近似计算`,
        updatedAt: '2024-12-08'
      }
    ]
  },
  {
    id: 'cs',
    name: '计算机基础',
    description: '数据结构、算法与操作系统基础',
    notes: [
      {
        id: 'cs-note-1',
        title: '数据结构综述',
        content: `1. 线性结构：数组、链表、栈、队列
2. 树与图：遍历、最短路与最小生成树
3. 哈希表与冲突解决策略
4. 排序算法复杂度对比
5. 常见面试题思路示例`,
        updatedAt: '2024-12-06'
      },
      {
        id: 'cs-note-2',
        title: '操作系统核心',
        content: `1. 进程与线程模型
2. 调度算法：RR、SJF、优先级
3. 内存管理：分页、分段、虚拟内存
4. 文件系统基本概念
5. 经典同步问题与信号量`,
        updatedAt: '2024-12-04'
      }
    ]
  }
]

export const findNoteById = (noteId) => {
  for (const subject of subjects) {
    const note = subject.notes.find((n) => n.id === noteId)
    if (note) {
      return { subject, note }
    }
  }
  return { subject: null, note: null }
}

