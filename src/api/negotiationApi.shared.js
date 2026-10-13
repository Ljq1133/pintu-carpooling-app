const toNumber = (value) => {
  if (value == null || value === '') return null
  const result = Number(value)
  return Number.isFinite(result) ? result : null
}

export function normalizeConversation(conversation) {
  if (!conversation || typeof conversation !== 'object') return conversation
  return {
    ...conversation,
    unreadCount: Number(conversation.unreadCount || 0)
  }
}

export function normalizeMessage(message) {
  if (!message || typeof message !== 'object') return message
  return {
    ...message,
    messageType: message.messageType || 'text',
    readAt: message.readAt || '',
    createdAt: message.createdAt || ''
  }
}

export function normalizeProposal(proposal) {
  if (!proposal || typeof proposal !== 'object') return proposal
  return {
    ...proposal,
    version: Number(proposal.version || 0),
    price: toNumber(proposal.price),
    passengerCount: Number(proposal.passengerCount || 0),
    pickupLng: toNumber(proposal.pickupLng),
    pickupLat: toNumber(proposal.pickupLat),
    confirmedRoles: Array.isArray(proposal.confirmedRoles) ? proposal.confirmedRoles : [],
    orderId: proposal.orderId || '',
    createdAt: proposal.createdAt || ''
  }
}

export function sortByNewestVersion(proposals) {
  return [...(proposals || [])].sort((left, right) => Number(right.version || 0) - Number(left.version || 0))
}

export function sortByCreatedAt(messages) {
  return [...(messages || [])].sort((left, right) => {
    const leftTime = Date.parse(String(left.createdAt || ''))
    const rightTime = Date.parse(String(right.createdAt || ''))
    if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) return 0
    if (Number.isNaN(leftTime)) return -1
    if (Number.isNaN(rightTime)) return 1
    return leftTime - rightTime
  })
}
