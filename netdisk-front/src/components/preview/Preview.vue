<template>
  <PreviewImage
    ref="imageViewerRef"
    :imageList="[imageUrl]"
    v-if="fileInfo.fileCategory == 3"
  ></PreviewImage>
  <Window
    :show="windowShow"
    @close="closeWindow"
    :width="fileInfo.fileCategory == 1 ? 1500 : 900"
    :title="fileInfo.fileName"
    :align="fileInfo.fileCategory == 1 ? 'center' : 'top'"
    v-else
  >
    <PreviewVideo :url="url" v-if="fileInfo.fileCategory == 1"></PreviewVideo>
    <PreviewExcel :url="url" v-if="fileInfo.fileType == 6"></PreviewExcel>
    <PreviewDoc :url="url" v-if="fileInfo.fileType == 5"></PreviewDoc>
    <PreviewPdf :url="url" v-if="fileInfo.fileType == 4"></PreviewPdf>
    <PreviewTxt
      :url="url"
      v-if="fileInfo.fileType == 7 || fileInfo.fileType == 8"
    ></PreviewTxt>
    <!--特殊预览-->
    <PreviewMusic
      :url="url"
      :fileName="fileInfo.fileName"
      v-if="fileInfo.fileCategory == 2"
    ></PreviewMusic>
    <PreviewDownload
      :createDownloadUrl="createDownloadUrl"
      :downloadUrl="downloadUrl"
      :fileInfo="fileInfo"
      v-if="fileInfo.fileCategory == 5 && fileInfo.fileType != 8"
    ></PreviewDownload>
  </Window>
</template>

<script setup>
import PreviewDoc from "@/components/preview/PreviewDoc.vue";
import PreviewDownload from "@/components/preview/PreviewDownload.vue";
import PreviewExcel from "@/components/preview/PreviewExcel.vue";
import PreviewImage from "@/components/preview/PreviewImage.vue";
import PreviewMusic from "@/components/preview/PreviewMusic.vue"
import PreviewPdf from "@/components/preview/PreviewPdf.vue";
import PreviewTxt from "@/components/preview/PreviewTxt.vue";
import PreviewVideo from "@/components/preview/PreviewVideo.vue";

import { ref, reactive, getCurrentInstance, nextTick, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const imageUrl = computed(() => {
  return (
      proxy.globalInfo.imageUrl + fileInfo.value.fileCover.replaceAll("_.", ".")
  );
});

const windowShow = ref(false);
const closeWindow = () => {
  windowShow.value = false;
};
const FILE_URL_MAP = {
  0: {
    fileUrl: "/file/getFile",
    videoUrl: "/file/ts/getVideoInfo",
    createDownloadUrl: "/file/createDownloadUrl",
    downloadUrl: "/api/file/download",
  },
  1: {
    fileUrl: "/admin/getFile",
    videoUrl: "/admin/ts/getVideoInfo",
    createDownloadUrl: "/admin/createDownloadUrl",
    downloadUrl: "/api/admin/download",
  },
  2: {
    fileUrl: "/showShare/getFile",
    videoUrl: "/showShare/ts/getVideoInfo",
    createDownloadUrl: "/showShare/createDownloadUrl",
    downloadUrl: "/api/showShare/download",
  },
  3: {
    fileUrl: "/file/getPublicFile",
    videoUrl: "/file/ts/getVideoInfo",
    createDownloadUrl: "/file/createPublicDownloadUrl",
    downloadUrl: "/api/file/publicDownload",
  }
};
const url = ref(null);
const createDownloadUrl = ref(null);
const downloadUrl = ref(null);
const fileInfo = ref({});

const imageViewerRef = ref();
const showPreview = (data, showPart) => {
  fileInfo.value = data;
  if (data.fileCategory == 3) {
    nextTick(() => {
      imageViewerRef.value.show(0);
    });
  } else {
    windowShow.value = true;
    if (data.url) {
      url.value = data.url;
    } else {
      let _url = FILE_URL_MAP[showPart].fileUrl;
      if (data.fileCategory == 1) {
        _url = FILE_URL_MAP[showPart].videoUrl;
      }
      if (showPart == 0) {
        _url += "/" + data.fileId;
      } else if (showPart == 1) {
        _url += "/" + data.userId + "/" + data.fileId;
      } else if (showPart == 2) {
        _url += "/" + data.shareId + "/" + data.fileId;
      } else if (showPart == 3) {
        _url += "/" + data.fileId;
      }
      url.value = _url;
    }

    let _createDownloadUrl = FILE_URL_MAP[showPart].createDownloadUrl;
    let _downloadUrl = FILE_URL_MAP[showPart].downloadUrl;
    if (showPart == 0) {
      _createDownloadUrl += "/" + data.fileId;
    } else if (showPart == 1) {
      _createDownloadUrl += "/" + data.userId + "/" + data.fileId;
    } else if (showPart == 2) {
      _createDownloadUrl += "/" + data.shareId + "/" + data.fileId;
    } else if (showPart == 3) {
      _createDownloadUrl += "/" + data.fileId;
    }
    createDownloadUrl.value = _createDownloadUrl;
    downloadUrl.value = _downloadUrl;
    console.log('showPart:', showPart, 'createDownloadUrl:', _createDownloadUrl, 'downloadUrl:', _downloadUrl);
  }
};
defineExpose({ showPreview });
</script>

<style lang="scss">
</style>