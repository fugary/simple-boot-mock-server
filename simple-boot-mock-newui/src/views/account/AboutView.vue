<script setup>
import { GITHUB_ADDRESS } from '@/config'
</script>
<template>
  <el-container class="flex-column">
    <strong>
      基于Spring Boot开发的Mock数据服务器
    </strong>
    <p>
      主要要两个项目:<br>
      1. 后台服务simple-boot-mock-server<br>
      2. UI界面simple-boot-mock-newui<br>
      最后打包的时候合并到一个项目中，成为一个jar包，最后打包成为一个zip文件，加压运行即可。
    </p>
    <p>
      GitHub源码：<el-link
        v-if="GITHUB_ADDRESS"
        :href="GITHUB_ADDRESS"
        type="primary"
        target="_blank"
      >
        {{ GITHUB_ADDRESS }}
      </el-link>
    </p>
  </el-container>
</template>

<style>
</style>
