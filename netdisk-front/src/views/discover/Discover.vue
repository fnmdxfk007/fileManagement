<template>
  <div>
    <!-- 顶部操作区 -->
    <div class="top">
      <div class="top-op">
        <div class="btn">
          <el-button
              type="primary"
              class="btn-setCollect"
              :disabled="selectFileIdList.length === 0"
              @click="confireSetCollect">
            <span class="iconfont icon-setCollect"></span>
            批量收藏
          </el-button>
        </div>
        <div class="search-refresh-group">
          <div class="search-panel-discover">
            <el-input
                clearable
                placeholder="输入文件名搜索"
                v-model="fileNameFuzzy"
                @keyup.enter="search"
                class="file-search-input"
                style=""
            >
              <template #suffix>
                <i class="iconfont icon-search" @click="search"></i>
              </template>
            </el-input>
          </div>
          <div class="iconfont icon-refresh" @click="loadPublicFileList"></div>
        </div>
      </div>
      <!--导航-->
      <Navigation ref="navigationRef" @navChange="navChange" />
    </div>
    <div class="file-list" v-if="tableData.list && tableData.list.length > 0">
      <Table
          ref="dataTableRef"
          :columns="columns"
          :showPagination="true"
          :dataSource="tableData"
          :fetch="loadPublicFileList"
          :initFetch="false"
          :options="tableOptions"
          @rowSelected="rowSelected"
      >
        <template #fileName="{ row }">
          <div
              class="file-item"
              @mouseenter="showOp(row)"
              @mouseleave="cancelShowOp(row)"
          >
            <template v-if="row.folderType === 1">
              <icon :fileType="0"></icon>
            </template>
            <template v-else>
              <icon :fileType="row.fileType"></icon>
            </template>
            <span class="file-name" :title="row.fileName" @click="preview(row)">
              {{ row.fileName }}
            </span>
          </div>
        </template>
        <template #nickName="{ row }">
          <span>{{ row.nickName }}</span>
        </template>
        <template #description="{ row }">
          <div class="description-container">
            <el-tooltip
                :content="row.description || '暂无简介'"
                placement="top"
            >
              <div class="description-text">
                {{ row.description || '暂无简介' }}
              </div>
            </el-tooltip>
          </div>
        </template>
        <template #lastUpdateTime="{ row }">
          <span>{{ row.lastUpdateTime }}</span>
        </template>
        <template #fileSize="{ row }">
          <span v-if="row.fileSize">
            {{ proxy.Utils.size2Str(row.fileSize) }}
          </span>
        </template>
        <template #actions="{ row }">
          <span
              v-if="row.status === 2 && row.folderType === 0"
              class="iconfont icon-download action-icon"
              @click="download(row)"
              @mouseenter="hoverAction = row.fileId"
              @mouseleave="hoverAction = ''"
              :class="{ 'is-hover': hoverAction === row.fileId }"
              style="margin-left: 8px;"
          >
            下载
          </span>
          <span
              v-if="row.status === 2 && row.folderType === 0"
              class="iconfont icon-preview action-icon"
              @click="preview(row)"
              @mouseenter="hoverPreview = row.fileId"
              @mouseleave="hoverPreview = ''"
              :class="{ 'is-hover': hoverPreview === row.fileId }"
              style="margin-left: 16px;"
          >
            预览
          </span>
        </template>
      </Table>
    </div>
    <div class="no-data" v-else>
      <div class="no-data-inner">
        <Icon iconName="no_data" :width="120" fit="fill"></Icon>
        <div class="tips">未找到相关文件，请尝试其他关键词</div>
      </div>
    </div>
    <!-- 预览 -->
    <Preview ref="previewRef" />
  </div>
</template>


<script setup>
import {ref, getCurrentInstance} from "vue";
import {useRouter, useRoute} from "vue-router";
import Preview from "@/components/preview/Preview.vue";
import Navigation from "@/components/Navigation.vue";
import Icon from "@/components/Icon.vue";

const {proxy} = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const fileNameFuzzy = ref("");
const currentFolder = ref({fileId: "0"});
const hoverAction = ref("");
const hoverPreview = ref("");
const selectFileIdList = ref([]);

const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName",
  },
  {
    label: "发布人",
    prop: "nickName",
    width: 250,
    align: "center",
    scopedSlots: "nickName",
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
    scopedSlots: "lastUpdateTime",
  },
  {
    label: "大小",
    prop: "fileSize",
    scopedSlots: "fileSize",
    width: 150,
    align: "center",
  },
  {
    label: "操作",
    prop: "actions",
    scopedSlots: "actions",
    width: 140,
    align: "center",
  },
];

const tableData = ref({
  list: [],
  pageNo: 1,
  pageSize: 15,
  pageTotal: 1,
  total: 0
});
const tableOptions = {
  extHeight: 50,
  selectType: "checkbox",
};

const api = {
  loadPublicFileList: "/file/loadPublicFileList",
  createPublicDownloadUrl: "/file/createPublicDownloadUrl",
  publicDownload: "/api/file/publicDownload",
  setCollect: "/file/setCollect",

};
const confireSetCollect = (type) => {
  proxy.Confirm(
      `是否将这些文件设为收藏？`,
      async () => {
        await setCollect(type);
      }
  );
};
const showOp = (row) => {
  tableData.value.list.forEach((element) => {
    element.showOp = false;
  });
  row.showOp = true;
};

const cancelShowOp = (row) => {
  row.showOp = false;
};

const previewRef = ref();
const navigationRef = ref();

const preview = (row) => {
  if (row.folderType == 1) {
    navigationRef.value.openFolder?.(row);
    return;
  }
  if (row.status != 2) {
    proxy.Message.warning("文件正在转码中，无法预览");
    return;
  }
  // 传递后端直链到 Preview 组件
  const fileUrl = `/file/getPublicFile/${row.fileId}`;
  previewRef.value.showPreview({ ...row, url: fileUrl }, 0);
};
const navChange = (data) => {
  const {curFolder} = data;
  currentFolder.value = curFolder || {fileId: "0"};
  loadPublicFileList();
};

const download = async (row) => {
  let result = await proxy.Request({
    url: api.createPublicDownloadUrl + "/" + row.fileId,
  });
  if (!result) {
    return;
  }
  window.location.href = api.publicDownload + "/" + result.data;
};

const rowSelected = (rows) => {
  selectFileIdList.value = [];
  rows.forEach((item) => {
    selectFileIdList.value.push(item.fileId);
  });
};

// 批量收藏
const setCollect = async () => {
  console.log('fileIds:', selectFileIdList.value.join(","));
  if (selectFileIdList.value.length === 0) {
    proxy.Message.warning("请先选择要收藏的文件");
    return;
  }
  const result = await proxy.Request({
    url: api.setCollect,
    params: {
      fileIds: selectFileIdList.value.join(",")
    }
  });
  if (result && result.code === 200) {
    proxy.Message.success("收藏成功！");
  } else {
    proxy.Message.error(result?.info || "收藏失败");
  }
};

const loadPublicFileList = async () => {
  const params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    fileNameFuzzy: fileNameFuzzy.value,
    filePid: currentFolder.value.fileId, // 当前文件夹 ID
  };
  const result = await proxy.Request({
    url: api.loadPublicFileList,
    showLoading: true,
    params,
  });
  if (result && result.data) {
    tableData.value = {
      ...result.data,
      list: Array.isArray(result.data.list) ? result.data.list : [],
    };
  }
};

const search = () => {
  loadPublicFileList();
};

// 初始化加载数据
loadPublicFileList();
</script>

<style scoped>
.top {
  display: flex;
  flex-direction: column;
  margin-bottom: 30px;
}

.top-op {
  display: flex;
  align-items: center;
  margin-top: 20px;
}

.btn {
  display: flex;
  align-items: center;
  margin-right: 10px;
}
.search-panel-discover {
  width: 300px;
}
.search-refresh-group {
  display: flex;
  align-items: center;
  flex: 1;
  margin-right: 800px;
}

.file-search-input {
  width: 100%;
}

.icon-refresh {
  cursor: pointer;
  margin-left: 10px;
}

.file-list {
  margin-top: 15px;
}

.file-item {
  display: flex;
  align-items: center;
}

.file-name {
  margin-left: 10px;
  flex: 1;
  cursor: pointer;
  color: #333;
  transition: color 0.2s;
}

.file-name:hover {
  color: #409eff;
  text-decoration: none;
}

.action-icon {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.action-icon.is-hover,
.action-icon:hover {
  color: #409eff;
  text-decoration: none;
}

.no-data {
  text-align: center;
  margin-top: 50px;
}

.description-container {
  min-width: 120px;
  max-width: 250px;
  width: 100%;
  display: flex;
  align-items: center;
}

.description-text {
  width: 100%;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
}

.description-text:hover {
  color: #409eff;
  text-decoration: none;
}
</style>