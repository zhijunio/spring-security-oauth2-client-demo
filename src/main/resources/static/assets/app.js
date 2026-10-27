const loginIcons = {
  'google-logo': ['G', 'login-icon-google'],
  'github-logo': ['GH', 'login-icon-github'],
  'facebook-logo': ['f', 'login-icon-facebook'],
  'dex-logo': ['D', 'login-icon-dex'],
  'apple-logo': ['A', 'login-icon-apple'],
}

document.querySelectorAll('[data-login-button-icon]').forEach((element) => {
  const iconName = element.dataset.loginButtonIcon?.trim().toLowerCase()
  if (!iconName) return
  const [mark, className] = loginIcons[iconName] ?? [iconName.replace(/-logo$/, '').slice(0, 2).toUpperCase(), '']
  element.textContent = mark
  if (className) element.classList.add(className)
})

const callButton = document.querySelector('#call-platform-api')

if (callButton) {
  const result = document.querySelector('#api-result')
  const resultTitle = document.querySelector('#result-title')
  const resultBody = document.querySelector('#result-body')
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content

  callButton.addEventListener('click', async () => {
    callButton.disabled = true
    callButton.querySelector('span:first-child').textContent = '正在请求平台 API…'
    result.hidden = true

    try {
      const tokenResponse = await fetch('/api/session/platform-token', {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken },
        credentials: 'same-origin',
      })
      if (!tokenResponse.ok) throw new Error('无法从当前会话读取平台令牌。')
      const { accessToken } = await tokenResponse.json()

      const apiResponse = await fetch('/api/me', {
        headers: { Authorization: `Bearer ${accessToken}` },
        credentials: 'omit',
      })
      const payload = await apiResponse.json()
      if (!apiResponse.ok) throw new Error(payload.detail ?? '平台 API 拒绝了该令牌。')

      resultTitle.textContent = '平台 API 已验证 JWT'
      resultBody.textContent = JSON.stringify(payload, null, 2)
    } catch (error) {
      resultTitle.textContent = '请求未完成'
      resultBody.textContent = error instanceof Error ? error.message : '网络请求失败，请重试。'
    } finally {
      result.hidden = false
      callButton.disabled = false
      callButton.querySelector('span:first-child').textContent = '再次调用平台 API'
    }
  })
}
