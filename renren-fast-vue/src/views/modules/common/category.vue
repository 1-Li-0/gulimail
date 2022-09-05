<template>
  <el-tree :data="menus" :props="defaultProps" node-key="catId" ref="menuTree"></el-tree>
</template>

<script>
export default {
  name: "category.vue",
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
    }
  },

  created() {
    this.getMenus()
  }
}
</script>

<style scoped>

</style>
