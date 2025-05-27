<template>
  <div>
    <div class="top">
      <div class="top-op">
        <div class="btn">
          <el-upload
            :show-file-list="false"
            :with-credentials="true"
            :multiple="true"
            :http-request="addFile"
            :accept="fileAccept"
          >
            <el-button type="primary">
              <span class="iconfont icon-upload"></span>
              上传
            </el-button>
          </el-upload>
        </div>
        <el-button type="success" @click="newFolder" v-if="category == 'all'">
          <span class="iconfont icon-folder-add"></span>
          新建文件夹
        </el-button>
        <el-button
          @click="delFileBatch"
          type="danger"
          :disabled="selectFileIdList.length == 0"
        >
          <span class="iconfont icon-del"></span>
          批量删除
        </el-button>
        <el-button
          @click="moveFolderBatch"
          type="warning"
          :disabled="selectFileIdList.length == 0"
        >
          <span class="iconfont icon-move"></span>
          批量移动
        </el-button>
        <!-- 批量公开/私有 按钮 -->
        <el-button
            type="primary"
            :disabled="selectFileIdList.length == 0"
            @click="confirmBatchSetPermission('public')"
        >
          <span class="iconfont icon-unlock"></span>
          批量公开
        </el-button>
        <el-button
            type="primary"
            :disabled="selectFileIdList.length == 0"
            @click="confirmBatchSetPermission('private')"
        >
          <span class="iconfont icon-lock"></span>
          批量个人
        </el-button>
        <div class="search-panel">
          <el-input
            clearable
            placeholder="输入文件名搜索"
            v-model="fileNameFuzzy"
            @keyup.enter="search"
          >
            <template #suffix>
              <i class="iconfont icon-search" @click="search"></i>
            </template>
          </el-input>
        </div>
        <div class="iconfont icon-refresh" @click="loadDataList"></div>
      </div>
      <!--导航-->
      <Navigation ref="navigationRef" @navChange="navChange"></Navigation>
    </div>
    <div class="file-list" v-if="tableData.list && tableData.list.length > 0">
      <Table
        ref="dataTableRef"
        :columns="columns"
        :showPagination="true"
        :dataSource="tableData"
        :fetch="loadDataList"
        :initFetch="false"
        :options="tableOptions"
        @rowSelected="rowSelected"
        :row-class-name="tableRowClassName"
      >
        <template #fileName="{ index, row }">
          <div
            class="file-item"
            @mouseenter="showOp(row)"
            @mouseleave="cancelShowOp(row)"
          >
            <template
              v-if="(row.fileType == 3 || row.fileType == 1) && row.status == 2"
            >
<!--              <icon :cover="row.fileCover" :width="32"></icon>-->
                  <icon :fileType="row.fileType"></icon>
            </template>
            <template v-else>
              <icon v-if="row.folderType == 0" :fileType="row.fileType"></icon>
              <icon v-if="row.folderType == 1" :fileType="0"></icon>
            </template>
            <span class="file-name" v-if="!row.showEdit" :title="row.fileName">
              <span @click="preview(row)">{{ row.fileName }}</span>
              <span v-if="row.status == 0" class="transfer-status">转码中</span>
              <span v-if="row.status == 1" class="transfer-status transfer-fail"
                >请下载后预览</span>
            </span>
            <div class="edit-panel" v-if="row.showEdit">
              <el-input
                v-model.trim="row.fileNameReal"
                ref="editNameRef"
                :maxLength="190"
                @keyup.enter="saveNameEdit(index)"
              >
                <template #suffix>{{ row.fileSuffix }}</template>
              </el-input>
              <span
                :class="[
                  'iconfont icon-right1',
                  row.fileNameReal ? '' : 'not-allow',
                ]"
                @click="saveNameEdit(index)"
              ></span>
              <span
                class="iconfont icon-error"
                @click="cancelNameEdit(index)"
              ></span>
            </div>
            <span class="op" >
              <template v-if="row.showOp && row.fileId && row.status == 2">
                <span class="iconfont icon-share1" @click="share(row)"
                  >分享</span
                >
                <span
                  class="iconfont icon-download"
                  @click="download(row)"
                  v-if="row.folderType == 0"
                  >下载</span
                >
                <span class="iconfont icon-del" @click="delFile(row)"
                  >删除</span
                >
                <span
                  class="iconfont icon-edit"
                  @click.stop="editFileName(index)"
                  >重命名</span
                >
                <span class="iconfont icon-move" @click="moveFolder(row)"
                  >移动</span
                >
              </template>
            </span>
          </div>
        </template>
        <template #fileSize="{ index, row }">
          <span v-if="row.fileSize">
            {{ proxy.Utils.size2Str(row.fileSize) }}</span
          >
        </template>
        <template #description="{ row }">
          <div
              class="description-container"
              @mouseenter="row.showTooltip = true"
              @mouseleave="row.showTooltip = false"
          >
            <!-- 非编辑状态 -->
            <el-tooltip
                v-if="!row.editing"
                :content="row.description || '请双击编辑'"
                placement="top"
            >
              <div
                  class="description-text"
                  :class="{ hover: row.showTooltip }"
                  @dblclick="editDescription(row)"
              >
                {{ row.description || '暂无简介' }}
              </div>
            </el-tooltip>
            <!-- 编辑状态 -->
            <el-input
                v-else
                v-model="row.description"
                :ref="(el) => (row.inputRef = el)"
                placeholder="请输入简介"
                @blur="saveDescription(row)"
                @keyup.enter="saveDescription(row)"
                @keydown.esc="cancelEditDescription(row)"
                @focus="handleFocus"
            />
          </div>
        </template>
      </Table>
    </div>
    <div class="no-data" v-else>
      <div class="no-data-inner">
        <Icon iconName="no_data" :width="120" fit="fill"></Icon>
        <div class="tips">当前目录为空，上传你的第一个文件吧</div>
        <div class="op-list">
          <el-upload
            :show-file-list="false"
            :with-credentials="true"
            :multiple="true"
            :http-request="addFile"
            :accept="fileAccept"
          >
            <div class="op-item">
              <Icon iconName="file" :width="60"></Icon>
              <div>上传文件</div>
            </div>
          </el-upload>
          <div class="op-item" v-if="category == 'all'" @click="newFolder">
            <Icon iconName="folder" :width="60"></Icon>
            <div>新建目录</div>
          </div>
        </div>
      </div>
    </div>
    <!--预览-->
    <Preview ref="previewRef"> </Preview>
    <!--移动-->
    <FolderSelect
      ref="folderSelectRef"
      @folderSelect="moveFolderDone"
    ></FolderSelect>
    <!--分享-->
    <FileShare ref="shareRef"></FileShare>
  </div>
</template>

<script setup>
import CategoryInfo from "@/js/CategoryInfo.js";
import FileShare from "./ShareFile.vue";
import { ref, reactive, getCurrentInstance, nextTick, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import axios from "axios";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const emit = defineEmits(["addFile"]);
//添加文件
const addFile = async (fileData) => {
  emit("addFile", { file: fileData.file, filePid: currentFolder.value.fileId });
};
//添加文件回调
const reload = () => {
  showLoading.value = false;
  loadDataList();
};
defineExpose({
  reload,
});

const currentFolder = ref({ fileId: 0 });

const api = {
  loadDataList: "/file/loadDataList",
  rename: "/file/rename",
  newFoloder: "/file/newFoloder",
  getFolderInfo: "/file/getFolderInfo",
  delFile: "/file/delFile",
  changeFileFolder: "/file/changeFileFolder",
  createDownloadUrl: "/file/createDownloadUrl",
  download: "/api/file/download",
  saveDescription: "/api/file/saveDescription",
  setPermission: "/api/file/setIsPublic"
};

const fileAccept = computed(() => {
  const categoryItem = CategoryInfo[category.value];
  return categoryItem ? categoryItem.accept : "*";
});

//列表   首页的设置
const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName",
  },
  {
    label: "文件简介",
    prop: "description",
    scopedSlots: "description",
    width: 250,
    align: "center",
  },
  {
    label: "修改时间",
    prop: "lastUpdateTime",
    width: 200,
    align: "center",
  },
  {
    label: "大小",
    prop: "fileSize",
    scopedSlots: "fileSize",
    width: 200,
    align: "center",
  },
];

const confirmBatchSetPermission = (type) => {
  proxy.Confirm(
      `是否将这些文件${type === 'public' ? '公开' : '设为个人'}？`,
      async () => {
        await batchSetPermission(type);
      }
  );
};
// 多文件状态切换
const batchSetPermission = async (type) => {
  if (selectFileIdList.value.length == 0) return;
  let permission = type === "public" ? 1 : 0;
  let accessType = type === "public" ? 0 : 1;
  let fileIds = selectFileIdList.value.join(",");
  let formData = new FormData();
  formData.append("fileId", fileIds);
  formData.append("permission", permission);
  formData.append("accessType", accessType);
  try {
    const res = await axios.post(api.setPermission, formData);
    const fileList = res.data?.data;
    if (Array.isArray(fileList)) {
      fileList.forEach(file => {
        let row = tableData.value.list.find(r => r.fileId === file.fileId);
        if (row) {
          row.permission = file.permission;
          row.accessType = file.accessType;
          row._permissionStatus = (file.permission == 1 && file.accessType == 0) ? 1 : 0;
        }
      });
    }
    proxy.Message.success(type === "public" ? "批量公开成功" : "批量私有成功");
  } catch (e) {
    proxy.Message.error("设置失败");
  }
};

const tableRowClassName = ({ row }) => {
  return row.permission == 1 && row.accessType == 0 ? 'public-row' : '';
};
// 简介部分
const editDescription = (row) => {
  row.originalDescription = row.description; // 保存原始值用于取消时回滚
  row.editing = true;
  nextTick(() => {
    row.inputRef?.focus();
    row.inputRef?.select(); // 直接通过动态 ref 操作输入框
  });
};
const saveDescription = async (row) => {
  console.log('fileId:', row.fileId, 'description:', row.description);
  row.editing = false;
  if (!row.description?.trim()) row.description = '';

  if (!row.fileId?.toString().trim()) {
    proxy.Message.error("文件ID不能为空！");
    return;
  }

  // 每次都新建一个 FormData 实例
  const formData = new FormData();
  formData.append('fileId', row.fileId.toString());
  formData.append('description', row.description ?? '');

  axios.post(api.saveDescription, formData)
      .then(res => {
        proxy.Message.success("文件简介已保存！");
      })
      .catch(error => {
        proxy.Message.error("请求失败：" + error.message);
      });
};
// 取消编辑
const cancelEditDescription = (row) => {
  row.editing = false;
  row.description = row.originalDescription; // 恢复原始值
};
//搜索
const search = () => {
  showLoading.value = true;
  loadDataList();
};
//列表
const tableData = ref({});
const tableOptions = {
  extHeight: 50,
  selectType: "checkbox",
};

const fileNameFuzzy = ref();
const showLoading = ref(true);
const category = ref();

const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    fileNameFuzzy: fileNameFuzzy.value,
    category: category.value,
    filePid: currentFolder.value.fileId,
  };
  if (params.category !== "all") {
    delete params.filePid;
  }
  let result = await proxy.Request({
    url: api.loadDataList,
    showLoading: showLoading,
    params,
  });
  if (!result) {
    return;
  }
  tableData.value = result.data;
  if (Array.isArray(tableData.value.list)) {
    tableData.value.list.forEach(row => {
      row._permissionStatus = (row.permission == 1 && row.accessType == 0) ? 1 : 0;
    });
  }
  editing.value = false;
  editing.value = false;
};

//展示操作按钮
const showOp = (row) => {
  tableData.value.list.forEach((element) => {
    element.showOp = false;
  });
  row.showOp = true;
};
const cancelShowOp = (row) => {
  row.showOp = false;
};

//编辑行
const editing = ref(false);
const editNameRef = ref();
//新建文件夹
const newFolder = () => {
  if (editing.value) {
    return;
  }
  tableData.value.list.forEach((element) => {
    element.showEdit = false;
  });
  editing.value = true;
  tableData.value.list.unshift({
    showEdit: true,
    fileType: 0,
    fileId: "",
    filePid: currentFolder.value.fileId,
  });
  nextTick(() => {
    editNameRef.value.focus();
  });
};

const cancelNameEdit = (index) => {
  const fileData = tableData.value.list[index];
  if (fileData.fileId) {
    fileData.showEdit = false;
  } else {
    tableData.value.list.splice(index, 1);
  }
  editing.value = false;
};

const saveNameEdit = async (index) => {
  const { fileId, filePid, fileNameReal } = tableData.value.list[index];
  if (fileNameReal == "" || fileNameReal.indexOf("/") != -1) {
    proxy.Message.warning("文件名不能为空且不能含有斜杠");
    return;
  }
  let url = api.rename;
  if (fileId == "") {
    url = api.newFoloder;
  }
  let result = await proxy.Request({
    url: url,
    params: {
      fileId,
      filePid: filePid,
      fileName: fileNameReal,
    },
  });
  if (!result) {
    return;
  }
  tableData.value.list[index] = result.data;
  editing.value = false;
};

//编辑文件名
const editFileName = (index) => {
  if (tableData.value.list[0].fileId == "") {
    tableData.value.list.splice(0, 1);
    index = index - 1;
  }
  tableData.value.list.forEach((element) => {
    element.showEdit = false;
  });
  let cureentData = tableData.value.list[index];
  cureentData.showEdit = true;

  //编辑文件
  if (cureentData.folderType == 0) {
    cureentData.fileNameReal = cureentData.fileName.substring(
      0,
      cureentData.fileName.indexOf(".")
    );
    cureentData.fileSuffix = cureentData.fileName.substring(
      cureentData.fileName.indexOf(".")
    );
  } else {
    cureentData.fileNameReal = cureentData.fileName;
    cureentData.fileSuffix = "";
  }
  editing.value = true;
  nextTick(() => {
    editNameRef.value.focus();
  });
};

//多选 批量选择
const selectFileIdList = ref([]);
const rowSelected = (rows) => {
  selectFileIdList.value = [];
  rows.forEach((item) => {
    selectFileIdList.value.push(item.fileId);
  });
};

//删除文件
const delFile = (row) => {
  proxy.Confirm(
    `你确定要删除【${row.fileName}】吗？删除的文件可在10天内通过回收站还原`,
    async () => {
      let result = await proxy.Request({
        url: api.delFile,
        params: {
          fileIds: row.fileId,
        },
      });
      if (!result) {
        return;
      }
      loadDataList();
    }
  );
};
//批量删除
const delFileBatch = () => {
  if (selectFileIdList.value.length == 0) {
    return;
  }
  proxy.Confirm(
    `你确定要删除这些文件吗？删除的文件可在10天内通过回收站还原`,
    async () => {
      let result = await proxy.Request({
        url: api.delFile,
        params: {
          fileIds: selectFileIdList.value.join(","),
        },
      });
      if (!result) {
        return;
      }
      loadDataList();
    }
  );
};

//移动目录
const folderSelectRef = ref();
const currentMoveFile = ref({});
const moveFolder = (data) => {
  currentMoveFile.value = data;
  folderSelectRef.value.showFolderDialog(currentFolder.value.fileId);
};

//批量移动
const moveFolderBatch = () => {
  currentMoveFile.value = {};
  folderSelectRef.value.showFolderDialog(currentFolder.value.fileId);
};

const moveFolderDone = async (folderId) => {
  if (
    currentMoveFile.value.filePid === folderId ||
    currentFolder.value.fileId == folderId
  ) {
    proxy.Message.warning("文件正在当前目录，无需移动");
    return;
  }
  let filedIdsArray = [];
  if (currentMoveFile.value.fileId) {
    filedIdsArray.push(currentMoveFile.value.fileId);
  } else {
    filedIdsArray = filedIdsArray.concat(selectFileIdList.value);
  }
  let result = await proxy.Request({
    url: api.changeFileFolder,
    params: {
      fileIds: filedIdsArray.join(","),
      filePid: folderId,
    },
  });
  if (!result) {
    return;
  }
  folderSelectRef.value.close();
  loadDataList();
};

const previewRef = ref();
const navigationRef = ref();
const preview = (data) => {
  if (data.folderType == 1) {
    navigationRef.value.openFolder(data);
    return;
  }
  if (data.status != 2) {
    proxy.Message.warning("文件正在转码中，无法预览");
    return;
  }
  previewRef.value.showPreview(data, 0);
};

//目录
const navChange = (data) => {
  const { curFolder, categoryId } = data;
  currentFolder.value = curFolder;
  showLoading.value = true;
  category.value = categoryId;
  loadDataList();
};

//下载文件
const download = async (row) => {
  let result = await proxy.Request({
    url: api.createDownloadUrl + "/" + row.fileId,
  });
  if (!result) {
    return;
  }
  window.location.href = api.download + "/" + result.data;
};
//分享
const shareRef = ref();
const share = (row) => {
  shareRef.value.show(row);
};
</script>

<style lang="scss" scoped>
@import "@/assets/file.list.scss";
.description-container {
  min-width: 120px;
  max-width: 250px;
  width: 100%;
  display: flex;
  align-items: center;
}
.description-text {
  width: 100%;
  cursor: pointer;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
  transition: color 0.2s;
}
.description-text.hover,
.description-text:hover {
  color: #409eff;
  text-decoration: underline;
}
.el-input {
  width: 100%;
}
</style>