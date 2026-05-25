import { useToast } from './useToast'

export function useApiError() {
  const { error, warning } = useToast()

  function handle(err) {
    const code = err.response?.data?.code
    const message = err.response?.data?.message

    switch (code) {
      case 'AUTH_002':
      case 'AUTH_003':
        // 자동 재발급 시도 중 — 토스트 없음
        break
      case 'AUTH_004':
        error('로그인이 만료되었습니다. 다시 로그인해주세요.')
        break
      case 'AUTH_011':
        warning('너무 많은 요청입니다. 잠시 후 다시 시도해주세요.')
        break
      case 'USER_002':
        error('이미 사용 중인 이메일입니다.')
        break
      case 'USER_003':
        error('이미 사용 중인 닉네임입니다.')
        break
      case 'ITEM_003':
        error('거래 가능하지 않은 상품입니다.')
        break
      case 'ITEM_005':
        warning('오늘은 이미 끌어올리기를 사용했습니다.')
        break
      case 'SLOT_003':
        error('다른 사용자가 예약 중입니다. 잠시 후 다시 시도해주세요.')
        break
      case 'SLOT_004':
        error('슬롯 잠금이 만료되었습니다. 다시 예약해주세요.')
        break
      case 'AI_002':
        warning('AI 기능 일일 사용 한도를 초과했습니다.')
        break
      case 'COMMON_005':
        warning('동시 요청 충돌이 발생했습니다. 다시 시도해주세요.')
        break
      default:
        error(message || '오류가 발생했습니다.')
    }
  }

  return { handle }
}
