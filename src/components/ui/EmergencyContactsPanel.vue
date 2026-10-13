<script setup>
import { reactive, ref } from 'vue'
import { ContactRound, Pencil, Plus, Save, Trash2, X } from 'lucide-vue-next'

defineProps({
  items: {
    type: Array,
    default: () => []
  },
  busyId: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['save', 'delete'])
const editingId = ref('')
const form = reactive({
  name: '',
  phone: '',
  relationship: '',
  isPrimary: false
})

function resetForm() {
  editingId.value = ''
  form.name = ''
  form.phone = ''
  form.relationship = ''
  form.isPrimary = false
}

function editContact(contact) {
  editingId.value = contact.id
  form.name = contact.name || ''
  form.phone = ''
  form.relationship = contact.relationship || ''
  form.isPrimary = Boolean(contact.isPrimary)
}

function submit() {
  emit('save', {
    id: editingId.value || undefined,
    name: form.name,
    phone: form.phone,
    relationship: form.relationship,
    isPrimary: form.isPrimary
  })
}
</script>

<template>
  <section class="contact-panel">
    <header>
      <ContactRound :size="18" />
      <strong>安全与隐私 · 紧急联系人</strong>
      <b>{{ items.length }}</b>
    </header>

    <p v-if="error" class="contact-error">{{ error }}</p>

    <div v-if="items.length" class="contact-list">
      <article v-for="item in items" :key="item.id">
        <div>
          <strong>{{ item.name }}</strong>
          <span>{{ item.relationship || '未填写关系' }} · {{ item.phoneMasked }}</span>
          <small v-if="item.isPrimary">主要联系人</small>
        </div>
        <button type="button" title="编辑联系人" :disabled="Boolean(busyId)" @click="editContact(item)">
          <Pencil :size="15" />
        </button>
        <button
          type="button"
          title="删除联系人"
          :disabled="Boolean(busyId)"
          @click="emit('delete', item.id)"
        >
          <Trash2 :size="15" />
        </button>
      </article>
    </div>
    <p v-else class="contact-empty">暂未设置紧急联系人。</p>

    <form @submit.prevent="submit">
      <div class="contact-form-head">
        <strong>{{ editingId ? '编辑联系人' : '新增联系人' }}</strong>
        <button v-if="editingId" type="button" title="取消编辑" @click="resetForm">
          <X :size="15" />
        </button>
      </div>
      <label>
        <span>姓名</span>
        <input v-model.trim="form.name" required maxlength="64" placeholder="联系人姓名">
      </label>
      <label>
        <span>手机号</span>
        <input
          v-model.trim="form.phone"
          :required="!editingId"
          inputmode="numeric"
          maxlength="11"
          pattern="1[3-9]\d{9}"
          :placeholder="editingId ? '留空保持原号码' : '11 位手机号码'"
        >
      </label>
      <label>
        <span>关系</span>
        <input v-model.trim="form.relationship" maxlength="32" placeholder="例如：家属、朋友">
      </label>
      <label class="contact-primary">
        <input v-model="form.isPrimary" type="checkbox">
        <span>设为主要联系人</span>
      </label>
      <button class="contact-save" type="submit" :disabled="Boolean(busyId)">
        <Save v-if="editingId" :size="16" />
        <Plus v-else :size="16" />
        <span>{{ busyId === 'save' ? '保存中...' : '保存联系人' }}</span>
      </button>
    </form>
  </section>
</template>

<style scoped>
.contact-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #E3E5E5;
  background: #FFFFFF;
}

.contact-panel header,
.contact-form-head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
}

.contact-panel header b,
.contact-list small {
  color: #16610A;
}

.contact-list,
.contact-panel form {
  display: grid;
  gap: 8px;
}

.contact-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36px 36px;
  gap: 8px;
  align-items: center;
  padding: 10px;
  background: #F7F8F8;
}

.contact-list article > div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.contact-list span,
.contact-list small,
.contact-empty,
.contact-panel label > span {
  color: #70747A;
  font-size: 12px;
}

.contact-list button,
.contact-form-head button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 0;
  background: #111111;
  color: #FFFFFF;
  cursor: pointer;
}

.contact-panel label {
  display: grid;
  gap: 5px;
}

.contact-panel input {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  border: 1px solid #D8DBDD;
  padding: 0 10px;
  color: #111111;
  font-weight: 700;
}

.contact-primary {
  grid-template-columns: 18px 1fr;
  align-items: center;
}

.contact-primary input {
  width: 16px;
  min-height: 16px;
  margin: 0;
}

.contact-save {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  background: #111111;
  color: #FFFFFF;
  font-weight: 900;
  cursor: pointer;
}

.contact-panel button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.contact-error {
  margin: 0;
  color: #B42318;
  font-size: 12px;
  font-weight: 800;
}

.contact-empty {
  margin: 0;
}
</style>
