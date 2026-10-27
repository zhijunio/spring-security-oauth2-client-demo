const csrfToken = document.querySelector('meta[name="_csrf"]')?.content
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content
const form = document.querySelector('#client-form')
const templatesSelect = document.querySelector('#client-template')
const mapperSelect = document.querySelector('#client-mapper')
const tenantSelect = document.querySelector('#client-tenant-id')
const message = document.querySelector('#form-message')
let templates = []
let tenants = []
let clientCatalog = []
let domains = []

async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  if (csrfToken && csrfHeader && options.method && options.method !== 'GET') headers.set(csrfHeader, csrfToken)
  const response = await fetch(path, { ...options, headers, credentials: 'same-origin' })
  if (!response.ok) throw new Error(`请求失败 (${response.status})`)
  const body = await response.text()
  return body ? JSON.parse(body) : null
}

function option(label, value) {
  const item = document.createElement('option')
  item.value = value
  item.textContent = label
  return item
}

async function loadCatalog() {
  ;[templates, tenants] = await Promise.all([
    request('/api/oauth2/config/template'),
    request('/api/tenants'),
  ])
  templatesSelect.replaceChildren(...templates.map((item) => option(`${item.name} · ${item.protocol}`, item.id)))
  mapperSelect.replaceChildren(...['BASIC', 'CUSTOM', 'GITHUB'].map((type) => option(type, type)))
  tenantSelect.replaceChildren(option('选择租户', ''), ...tenants.map((item) =>
    option(item.systemTenant ? `${item.name} · 按登录身份映射租户` : item.name, item.id)))
  document.querySelector('#domain-tenant-id').replaceChildren(option('选择租户', ''), ...tenants
    .filter((item) => !item.systemTenant)
    .map((item) => option(item.name, item.id)))
  applyTemplateDefaults()
}

function renderDomainClientOptions(selectedIds = []) {
  const options = document.querySelector('#domain-client-options')
  if (!clientCatalog.length) {
    options.replaceChildren(Object.assign(document.createElement('p'), { textContent: '先创建 OAuth2 Client。', className: 'muted' }))
    return
  }
  options.replaceChildren(...clientCatalog.map((client) => {
    const label = document.createElement('label')
    label.className = 'domain-client-option'
    const checkbox = document.createElement('input')
    checkbox.type = 'checkbox'
    checkbox.value = client.id
    checkbox.checked = selectedIds.includes(client.id)
    const text = document.createElement('span')
    text.textContent = `${client.name} · ${client.providerName}`
    label.append(checkbox, text)
    return label
  }))
}

function applyTemplateDefaults() {
  const template = templates.find((item) => item.id === templatesSelect.value)
  if (!template) return
  const appleTemplate = template.mapperConfig?.type === 'APPLE'
  mapperSelect.closest('label').hidden = appleTemplate
  if (!appleTemplate) mapperSelect.value = template.mapperConfig?.type || 'BASIC'
  document.querySelector('#client-allow-user-creation').checked = template.mapperConfig?.allowUserCreation ?? true
  document.querySelector('#client-activate-user').checked = template.mapperConfig?.activateUser ?? true
  const basic = template.mapperConfig?.basic
  if (basic) {
    const defaults = [
      ['#basic-email-attribute-key', basic.emailAttributeKey],
      ['#basic-first-name-attribute-key', basic.firstNameAttributeKey],
      ['#basic-last-name-attribute-key', basic.lastNameAttributeKey],
      ['#basic-tenant-name-strategy', basic.tenantNameStrategy],
      ['#basic-tenant-name-pattern', basic.tenantNamePattern],
      ['#basic-customer-name-pattern', basic.customerNamePattern],
    ]
    defaults.forEach(([selector, value]) => {
      const field = document.querySelector(selector)
      if (!field.value && value) field.value = value
    })
  }
  const custom = template.mapperConfig?.custom
  if (custom) {
    if (!document.querySelector('#custom-url').value) document.querySelector('#custom-url').value = custom.url || ''
    if (!document.querySelector('#custom-username').value) document.querySelector('#custom-username').value = custom.username || ''
    if (custom.sendToken) document.querySelector('#custom-send-token').checked = true
  }
  if (!document.querySelector('#client-scopes').value) document.querySelector('#client-scopes').value = template.scope.join(',')
  if (!document.querySelector('#client-button-icon').value) document.querySelector('#client-button-icon').value = template.loginButtonIcon || ''
  if (!document.querySelector('#client-button-label').value) document.querySelector('#client-button-label').value = template.loginButtonLabel || ''
}

async function loadClients() {
  const list = document.querySelector('#client-list')
  list.replaceChildren(Object.assign(document.createElement('p'), { textContent: '正在加载客户端…', className: 'muted' }))
  try {
    const page = await request('/api/oauth2/client/infos?pageSize=100&page=0')
    const clients = page.data
    clientCatalog = clients
    const selectedIds = [...document.querySelectorAll('#domain-client-options input:checked')].map((item) => item.value)
    renderDomainClientOptions(selectedIds)
    if (!clients.length) {
      list.replaceChildren(Object.assign(document.createElement('p'), { textContent: '尚无 OAuth2 Client。使用右侧模板创建。', className: 'muted' }))
      return
    }
    list.replaceChildren(...clients.map((client) => {
      const row = document.createElement('article')
      row.className = 'client-row'
      const details = document.createElement('div')
      const nameElement = document.createElement('strong')
      nameElement.textContent = client.name
      const meta = document.createElement('p')
      meta.textContent = client.providerName
      details.append(nameElement, meta)
      const actions = document.createElement('div')
      actions.className = 'row-actions'
      const edit = document.createElement('button')
      edit.type = 'button'; edit.textContent = '编辑'; edit.addEventListener('click', async () => {
        try { editClient(await request(`/api/oauth2/client/${client.id}`)) }
        catch (error) { showMessage(error.message, true) }
      })
      const remove = document.createElement('button')
      remove.type = 'button'; remove.textContent = '删除'; remove.addEventListener('click', async () => {
        if (!window.confirm(`删除 ${client.name}？`)) return
        try { await request(`/api/oauth2/client/${client.id}`, { method: 'DELETE' }); await loadClients() }
        catch (error) { showMessage(error.message, true) }
      })
      actions.append(edit, remove)
      row.append(details, actions)
      return row
    }))
  } catch (error) {
    list.replaceChildren(Object.assign(document.createElement('p'), { textContent: error.message, className: 'notice-error' }))
  }
}

async function loadDomains() {
  const list = document.querySelector('#domain-list')
  try {
    const page = await request('/api/domain/infos?pageSize=100&page=0')
    domains = page.data
    if (!domains.length) {
      list.replaceChildren(Object.assign(document.createElement('p'), { textContent: '尚未配置登录域名。新增域名并关联 Client 后，登录页才会显示对应登录方式。', className: 'muted' }))
      return
    }
    list.replaceChildren(...domains.map((domain) => {
      const row = document.createElement('article')
      row.className = 'client-row'
      const details = document.createElement('div')
      const nameElement = document.createElement('strong')
      nameElement.textContent = domain.name
      const meta = document.createElement('p')
      const tenant = tenants.find((item) => item.id === domain.tenantId)
      meta.textContent = `${tenant?.name || domain.tenantId} · ${domain.oauth2Enabled ? 'OAuth2 已启用' : 'OAuth2 已停用'} · ${domain.oauth2ClientInfos.length} 个 Client`
      details.append(nameElement, meta)
      const actions = document.createElement('div')
      actions.className = 'row-actions'
      const edit = document.createElement('button')
      edit.type = 'button'; edit.textContent = '编辑'; edit.addEventListener('click', () => editDomain(domain))
      const remove = document.createElement('button')
      remove.type = 'button'; remove.textContent = '删除'; remove.addEventListener('click', async () => {
        if (!window.confirm(`删除域名 ${domain.name}？`)) return
        try { await request(`/api/domain/${domain.id}`, { method: 'DELETE' }); await loadDomains() }
        catch (error) { showDomainMessage(error.message, true) }
      })
      actions.append(edit, remove)
      row.append(details, actions)
      return row
    }))
  } catch (error) {
    list.replaceChildren(Object.assign(document.createElement('p'), { textContent: error.message, className: 'notice-error' }))
  }
}

function editDomain(domain) {
  document.querySelector('#domain-id').value = domain.id
  document.querySelector('#domain-name').value = domain.name
  document.querySelector('#domain-tenant-id').value = domain.tenantId
  document.querySelector('#domain-oauth2-enabled').checked = domain.oauth2Enabled
  renderDomainClientOptions(domain.oauth2ClientInfos.map((client) => client.id))
  document.querySelector('#domain-submit-label').textContent = '更新域名'
  document.querySelector('#domain-form').scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function resetDomainForm() {
  document.querySelector('#domain-form').reset()
  document.querySelector('#domain-id').value = ''
  document.querySelector('#domain-submit-label').textContent = '保存域名'
  renderDomainClientOptions()
  showDomainMessage('')
}

function showDomainMessage(text, isError = false) {
  const message = document.querySelector('#domain-message')
  message.textContent = text
  message.className = isError ? 'form-message is-error' : 'form-message is-success'
}

function editClient(client) {
  document.querySelector('#form-title').textContent = '编辑登录方式'
  document.querySelector('#client-id').value = client.id
  document.querySelector('#registration-id').value = client.id
  updateCallbackPreview()
  document.querySelector('#client-name').value = client.name
  tenantSelect.value = client.tenantId || ''
  templatesSelect.value = client.templateId
  const mapperConfig = client.mapperConfig || {}
  applyTemplateDefaults()
  if (mapperConfig.type !== 'APPLE') mapperSelect.value = mapperConfig.type || ''
  document.querySelector('#oauth-client-id').value = client.clientId
  document.querySelector('#oauth-client-secret').value = ''
  document.querySelector('#oauth-client-id').placeholder = ''
  document.querySelector('#oauth-client-secret').placeholder = '重新输入 Secret（不会回显）'
  document.querySelector('#client-scopes').value = client.scope.join(',')
  document.querySelector('#client-button-icon').value = client.loginButtonIcon || ''
  document.querySelector('#client-button-label').value = client.loginButtonLabel || ''
  document.querySelector('#client-allow-user-creation').checked = mapperConfig.allowUserCreation
  document.querySelector('#client-activate-user').checked = mapperConfig.activateUser
  document.querySelector('#basic-email-attribute-key').value = mapperConfig.basic?.emailAttributeKey || ''
  document.querySelector('#basic-first-name-attribute-key').value = mapperConfig.basic?.firstNameAttributeKey || ''
  document.querySelector('#basic-last-name-attribute-key').value = mapperConfig.basic?.lastNameAttributeKey || ''
  document.querySelector('#basic-tenant-name-strategy').value = mapperConfig.basic?.tenantNameStrategy || ''
  document.querySelector('#basic-tenant-name-pattern').value = mapperConfig.basic?.tenantNamePattern || ''
  document.querySelector('#basic-customer-name-pattern').value = mapperConfig.basic?.customerNamePattern || ''
  document.querySelector('#custom-url').value = mapperConfig.custom?.url || ''
  document.querySelector('#custom-username').value = mapperConfig.custom?.username || ''
  document.querySelector('#custom-password').value = ''
  document.querySelector('#custom-send-token').checked = mapperConfig.custom?.sendToken || false
  document.querySelector('#client-enabled').checked = client.enabled
  document.querySelector('#submit-label').textContent = '更新 Client'
  document.querySelector('#cancel-edit').hidden = false
  document.querySelector('#oauth-client-id').required = false
  document.querySelector('#oauth-client-secret').required = false
  form.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function resetForm() {
  form.reset()
  document.querySelector('#client-id').value = ''
  document.querySelector('#registration-id').value = crypto.randomUUID()
  updateCallbackPreview()
  document.querySelector('#form-title').textContent = '添加登录方式'
  document.querySelector('#submit-label').textContent = '保存 Client'
  document.querySelector('#cancel-edit').hidden = true
  document.querySelector('#oauth-client-id').required = true
  document.querySelector('#oauth-client-secret').required = true
  document.querySelector('#oauth-client-id').placeholder = ''
  document.querySelector('#oauth-client-secret').placeholder = ''
  applyTemplateDefaults()
}

function updateCallbackPreview() {
  document.querySelector('#callback-uri').textContent = `${window.location.origin}/login/oauth2/code/${document.querySelector('#registration-id').value}`
}

function showMessage(text, isError = false) {
  message.textContent = text
  message.className = isError ? 'form-message is-error' : 'form-message is-success'
}

templatesSelect.addEventListener('change', applyTemplateDefaults)
document.querySelector('#refresh-clients').addEventListener('click', loadClients)
document.querySelector('#refresh-domains').addEventListener('click', loadDomains)
document.querySelector('#cancel-edit').addEventListener('click', resetForm)
document.querySelector('#reset-domain').addEventListener('click', resetDomainForm)
document.querySelector('#domain-form').addEventListener('submit', async (event) => {
  event.preventDefault()
  const clientIds = [...document.querySelectorAll('#domain-client-options input:checked')].map((item) => item.value)
  const query = new URLSearchParams({ oauth2ClientIds: clientIds.join(',') })
  const payload = {
    id: document.querySelector('#domain-id').value || null,
    name: document.querySelector('#domain-name').value.trim(),
    tenantId: document.querySelector('#domain-tenant-id').value,
    oauth2Enabled: document.querySelector('#domain-oauth2-enabled').checked,
  }
  try {
    await request(`/api/domain?${query}`, { method: 'POST', body: JSON.stringify(payload) })
    showDomainMessage('域名与登录方式已保存。')
    resetDomainForm()
    await loadDomains()
  } catch (error) { showDomainMessage(error.message, true) }
})
document.querySelector('#create-tenant').addEventListener('click', async () => {
  const name = window.prompt('输入租户名称')?.trim()
  if (!name) return
  try {
    const tenant = await request('/api/tenants', { method: 'POST', body: JSON.stringify({ name }) })
    tenants.push(tenant)
    tenants.sort((left, right) => left.name.localeCompare(right.name))
    tenantSelect.replaceChildren(option('选择租户', ''), ...tenants.map((item) =>
      option(item.systemTenant ? `${item.name} · 按登录身份映射租户` : item.name, item.id)))
    tenantSelect.value = tenant.id
    showMessage('租户已创建。')
  } catch (error) { showMessage(error.message, true) }
})
document.querySelector('#copy-callback').addEventListener('click', async () => {
  await navigator.clipboard.writeText(document.querySelector('#callback-uri').textContent)
  showMessage('回调 URI 已复制到剪贴板。')
})
form.addEventListener('submit', async (event) => {
  event.preventDefault()
  const secret = document.querySelector('#oauth-client-secret').value
  const payload = {
    id: document.querySelector('#registration-id').value,
    name: document.querySelector('#client-name').value,
    templateId: templatesSelect.value,
    clientId: document.querySelector('#oauth-client-id').value,
    clientSecret: secret,
    scope: document.querySelector('#client-scopes').value.split(',').map((value) => value.trim()).filter(Boolean),
    tenantId: tenantSelect.value || null,
    loginButtonIcon: document.querySelector('#client-button-icon').value.trim() || null,
    loginButtonLabel: document.querySelector('#client-button-label').value.trim() || null,
    mapperConfig: {
      type: templates.find((item) => item.id === templatesSelect.value)?.mapperConfig?.type === 'APPLE'
        ? 'APPLE'
        : mapperSelect.value,
      allowUserCreation: document.querySelector('#client-allow-user-creation').checked,
      activateUser: document.querySelector('#client-activate-user').checked,
      basic: {
        emailAttributeKey: document.querySelector('#basic-email-attribute-key').value.trim() || null,
        firstNameAttributeKey: document.querySelector('#basic-first-name-attribute-key').value.trim() || null,
        lastNameAttributeKey: document.querySelector('#basic-last-name-attribute-key').value.trim() || null,
        tenantNameStrategy: document.querySelector('#basic-tenant-name-strategy').value || null,
        tenantNamePattern: document.querySelector('#basic-tenant-name-pattern').value.trim() || null,
        customerNamePattern: document.querySelector('#basic-customer-name-pattern').value.trim() || null,
      },
      custom: {
        url: document.querySelector('#custom-url').value.trim() || null,
        username: document.querySelector('#custom-username').value.trim() || null,
        password: document.querySelector('#custom-password').value || null,
        sendToken: document.querySelector('#custom-send-token').checked,
      },
    },
    enabled: document.querySelector('#client-enabled').checked,
  }
  try {
    await request('/api/oauth2/client', { method: 'POST', body: JSON.stringify(payload) })
    showMessage('客户端已保存。回到首页可以看到启用的登录方式。')
    resetForm()
    await loadClients()
  } catch (error) { showMessage(error.message, true) }
})

resetForm()
loadCatalog().then(async () => {
  await loadClients()
  await loadDomains()
}).catch((error) => showMessage(error.message, true))
