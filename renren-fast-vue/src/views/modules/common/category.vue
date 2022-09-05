<template>
  <el-tree :data="menus" :props="defaultProps" node-key="catId" ref="menuTree" @node-click="nodeClick"></el-tree>
</template>

<script>
export default {
  comments: {},
  props: {},
  data() {
    return {
      menus: [], //节点菜单

      defaultProps: {
        children: 'children',
        label: 'name'
      }
    };
  },
  methods: {
    getMenus() {
      this.$http({
        url: this.$http.adornUrl('/product/category/list/tree'),
        method: 'get'
      }).then(({data}) => {
        this.menus = data.data;
      });
    },
    nodeClick(data, node, component) {
      this.$emit("tree-node-click", data, node, component);
    }
  },

  created() {
    this.getMenus()
  }
}
</script>

<style scoped>

</style>
