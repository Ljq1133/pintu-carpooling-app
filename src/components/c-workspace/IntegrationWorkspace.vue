<script setup>
import { Braces, KeyRound, ListChecks } from 'lucide-vue-next'
import { cGroupApiNeeds, demoAccounts } from '../../mock/cGroupData'
</script>

<template>
  <div class="integration-workspace">
    <section class="work-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">API Contract</span>
          <h3>C 组需要 A 组优先提供的接口</h3>
        </div>
        <Braces :size="28" />
      </div>
      <div class="api-table">
        <div v-for="[method, path, desc] in cGroupApiNeeds" :key="path" class="api-row">
          <b>{{ method }}</b>
          <code>{{ path }}</code>
          <span>{{ desc }}</span>
        </div>
      </div>
    </section>

    <section class="work-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">Demo Accounts</span>
          <h3>演示账号与用途</h3>
        </div>
        <KeyRound :size="28" />
      </div>
      <div class="account-list">
        <article v-for="[role, username, password, usage] in demoAccounts" :key="username">
          <strong>{{ role }}</strong>
          <code>{{ username }} / {{ password }}</code>
          <span>{{ usage }}</span>
        </article>
      </div>
    </section>

    <section class="work-panel wide-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">Integration Checklist</span>
          <h3>等 AB 开始后你的联调清单</h3>
        </div>
        <ListChecks :size="28" />
      </div>
      <ol class="checklist">
        <li>确认 A 组订单状态字段是否使用：待匹配、协商中、已确认、履约中、待付款、已完成、已取消。</li>
        <li>确认 B 组乘客端发起请求后，后端是否生成 `match_request` 并推送给司机端。</li>
        <li>司机同意请求后，确认订单是否进入“协商中”，并能在管理端订单看板出现。</li>
        <li>车辆审核通过后，司机端车辆列表中对应车辆状态应同步为“已认证”。</li>
        <li>信用分低于 60 分时，管理端限制状态应能影响乘客发单或司机接单。</li>
      </ol>
    </section>
  </div>
</template>

