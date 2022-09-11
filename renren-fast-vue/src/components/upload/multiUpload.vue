<template> 
  <div>
    <el-upload
      action="http://localhost:88/api/product/brand/upload"
      list-type="picture"
      name="picture"
      :multiple="false" :show-file-list="showFileList"
      :file-list="fileList"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :on-success="handleUploadSuccess"
      :on-preview="handlePreview">
      <el-button size="small" type="primary">点击上传</el-button>
      <div slot="tip" class="el-upload__tip">只能上传jpg/png文件，且不超过10MB</div>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible">
      <img width="100%" :src="file.url" alt="">
    </el-dialog>
  </div>
</template>
<script>

export default {
  name: 'MultiUpload',
  props: {
    value: ""
  },
  computed: {
    imageUrl() {
      return this.value;
    },
    imageName() {
      if (this.value != null && this.value !== '') {
        return this.value.toString().substr(this.value.lastIndexOf("/") + 1);
      } else {
        return null;
      }
    },
    fileList() {
      return [{
        name: this.imageName,
        url: this.imageUrl
      }]
    },
    showFileList: {
      get: function () {
        return this.value !== null && this.value !== ''&& this.value!==undefined;
      },
      set: function (newValue) {
      }
    }
  },
  data() {
    return {
      file: {url: "",name: ""},
      dialogVisible: false
    };
  },
  methods: {
    //验证文件
    beforeUpload(picture){

    },
    emitInput(val) {
      this.$emit('input', val)
    },

    handleRemove(file, fileList) {
      this.emitInput('');
    },
    handlePreview(file) {
      this.dialogVisible = true;
    },
    handleUploadSuccess(res, file) {
      this.file.name=file.name;
      console.log("上传成功...")
      this.showFileList = true;
      this.fileList.pop();
      this.file.url = "http://192.168.12.128:8888/"+res.path;
      this.emitInput(this.file.url);
    }
  }
}
</script>
<style>

</style>


