<template>
  <div>
    <el-switch v-model="draggable" active-text="开启拖拽" inactive-text="关闭拖拽"></el-switch>

    <el-button v-if="draggable" @click="batchSave">保存菜单</el-button>
    <el-button type="danger" @click="batchDelete">批量删除</el-button>

    <el-tree :data="menus"
             :props="defaultProps"
             :expand-on-click-node="false"
             show-checkbox
             node-key="catId"
             :default-expanded-keys="expandedKey"
             draggable
             :draggable="draggable"
             :allow-drop="allowDrop"
             @node-drop="handleDrop"
             ref="menuTree"
    >
     <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button v-if="node.level <=2" type="text" size="mini" @click="() => append(data)">Append</el-button>
          <el-button type="text" size="mini" @click="edit(data)">edit</el-button>
          <el-button v-if="node.childNodes.length==0" type="text" size="mini"
                     @click="() => remove(node, data)">Delete</el-button>
        </span>
      </span>
    </el-tree>

    <el-dialog :title="title" :visible.sync="dialogFormVisible" width="30%" :close-on-click-modal="false">
      <el-form :model="category">
        <el-form-item label="分类名称">
          <el-input v-model="category.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="category.icon" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="category.productUnit" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitData">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "category",
  comments: {},
  props: {},
  data() {
    return {
      draggable: false,
      updateNodes: [], //收集拖拽后节点数组的信息进行更新
      maxLevel: 0,
      title: "",
      dialogType: "", //复用弹框，区分调用时需要给弹框定义一个类型
      menus: [], //节点菜单
      expandedKey: [], //默认展开节点，保持增删改节点时的展开状态
      dialogFormVisible: false, //dialog弹框是否显示
      //为单个节点对象赋予属性
      category: {catId: null, name: "", parentCid: 0, catLevel: 0, showStatus: 1, sort: 0, icon: "", productUnit: ""},

      defaultProps: {
        children: 'children',
        label: 'name'
      }
    };
  },
  methods: {
    //批量删除
    batchDelete() {
      let catIds = this.$refs.menuTree.getCheckedNodes().map(node => node.catId);
      let menuNames = this.$refs.menuTree.getCheckedNodes().map(node => node.name);
      this.$confirm(`是否批量删除【${menuNames}】菜单?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http({
          url: this.$http.adornUrl(`/product/category/delete`),
          method: 'post',
          data: this.$http.adornData(catIds, false)
        }).then(({data}) => {
          this.$message({
            showClose: true,
            message: '菜单批量删除成',
            type: 'success'
          });
          //成功时，刷新菜单
          this.getMenus();
        });
      }).catch(() => {});
    },

    //批量保存菜单拖拽修改
    batchSave() {
      //信息收集完成，发送请求修改数据库
      this.$http({
        url: this.$http.adornUrl(`/product/category/update/sort`),
        method: 'post',
        data: this.$http.adornData(this.updateNodes, false)
      }).then(({data}) => {
        this.$message({
          showClose: true,
          message: '菜单顺序修改成功',
          type: 'success'
        });
      });
      //刷新菜单，清空收集的数据
      this.getMenus();
      this.updateNodes = [];
    },
    //拖拽完成后,处理拖拽节点的数据
    handleDrop(draggingNode, dropNode, dropType, event) {
      //获取目标节点的父节点
      var parentNode = null;
      dropType === "inner" ? parentNode = dropNode : parentNode = dropNode.parent;
      //重新对拖拽节点及其兄弟节点排序
      let siblings = parentNode.childNodes;
      for (let i = 0; i < siblings.length; i++) {
        //如果当前节点是被拖拽的节点，需要更新父节点id、层级和排序信息
        if (siblings[i].data.catId === draggingNode.data.catId) {
          this.updateNodes.push({
            catId: siblings[i].data.catId,
            parentCid: parentNode.data.catId,
            catLevel: siblings[i].level,
            sort: i
          })
          //被拖拽的节点还需要修改其子节点的层级信息
          this.updateChildNodes(siblings[i]);
        } else {
          this.updateNodes.push({catId: siblings[i].data.catId, sort: i})
        }
      }
      //拖拽后，展开目标节点数组 (可能拖拽多个节点)
      this.expandedKey.push(parentNode.data.catId);
    },
    //修改子节点
    updateChildNodes(node) {
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          var cNode = node.childNodes[i];
          this.updateNodes.push({catId: cNode.data.catId, catLevel: cNode.level});
          this.updateChildNodes(cNode);
        }
      }
    },
    //允许拖拽功能
    allowDrop(draggingNode, dropNode, type) {
      //计算最大深度
      this.countMaxLevel(draggingNode);
      let deep = this.maxLevel - draggingNode.level + 1;
      //深度计算完成，重置maxLevel的值
      this.maxLevel = 0;
      //判断
      if (type === "inner") {
        return (deep + dropNode.level) <= 3;
      } else {
        return (deep + dropNode.parent.level) <= 3;
      }
    },
    //计算节点最大层数
    countMaxLevel(node) {
      //初始化值为当前节点的深度
      this.maxLevel = node.level;
      //判断是否有子节点
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          if (this.maxLevel < node.childNodes[i].level) {
            this.maxLevel = node.childNodes[i].level;
          }
          this.countMaxLevel(node.childNodes[i]);
        }
      }
    },

    edit(data) {
      this.dialogType = "edit";
      this.title = "修改";
      this.dialogFormVisible = true;

      //显示数据库中的原始数据
      this.$http({
        url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
        method: 'get',
      }).then(({data}) => {
        this.category.name = data.category.name;
        this.category.catId = data.category.catId;
        this.category.icon = data.category.icon;
        this.category.productUnit = data.category.productUnit;
        this.category.parentCid = data.category.parentCid;
      });
    },

    append(data) {
      this.category.name = "";
      this.category.icon = "";
      this.category.catId = "";
      this.category.parentCid = "";
      this.category.productUnit = "";

      this.dialogType = "add";
      this.title = "新增";
      this.category.parentCid = data.catId;
      this.category.catLevel = data.catLevel * 1 + 1;
      this.dialogFormVisible = true;
    },

    submitData() {
      if (this.dialogType == "add") {
        this.addCategory();
      }
      if (this.dialogType == "edit") {
        this.editCategory();
      }
    },

    addCategory() {
      this.$http({
        url: this.$http.adornUrl('/product/category/save'),
        method: 'post',
        data: this.$http.adornData(this.category, false)
      }).then(({data}) => {
        this.$message({
          showClose: true,
          message: '菜单保存成功',
          type: 'success'
        });
        this.dialogFormVisible = false;
        this.getMenus();
        this.expandedKey = [this.category.parentCid];
      });
    },

    editCategory() {
      let {catId, name, parentCid, icon, productUnit} = this.category;
      this.$http({
        url: this.$http.adornUrl(`/product/category/update`),
        method: 'post',
        data: this.$http.adornData({catId, parentCid, name, icon, productUnit}, false)
      }).then(({data}) => {
        this.$message({
          showClose: true,
          message: '菜单修改成功',
          type: 'success'
        });
        this.dialogFormVisible = false;
        this.getMenus();
        this.expandedKey = [this.category.parentCid];
      });
    },

    remove(node, data) {
      let ids = [data.catId];
      this.$confirm(`是否删除【${data.name}】菜单?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http({
          url: this.$http.adornUrl('/product/category/deleteMenu'),
          method: 'post',
          data: this.$http.adornData(ids, false)
        }).then(({data}) => {
          this.$message({
            showClose: true,
            message: '菜单删除成功',
            type: 'success'
          });
          //展开父节点
          this.expandedKey = [node.parent.data.catId];
          //刷新菜单
          this.getMenus();
        });
      }).catch(() => {
        this.$message({
          showClose: true,
          message: '菜单删除失败',
          type: 'error'
        });
      });
    },
    //刷新菜单
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

<style>

</style>
