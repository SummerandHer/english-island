import { READING_EXAM_SECONDS, useExamTimer } from '~/composables/useExamTimer'

export function useReadingTimer(onTimeout: () => void) {
  return useExamTimer(READING_EXAM_SECONDS, onTimeout)
}
