import { ref } from 'vue'
import axios from 'axios'
import { itemApi } from '@/api/item'
import { useToast } from './useToast'

const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

export function useImageUpload() {
  const uploading = ref(false)
  const progresses = ref({})
  const { error } = useToast()

  function validate(files) {
    for (const file of files) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        error(`${file.name}: 지원하지 않는 형식입니다. (JPG, PNG, WEBP, GIF만 가능)`)
        return false
      }
      if (file.size > MAX_FILE_SIZE) {
        error(`${file.name}: 파일 크기가 5MB를 초과합니다.`)
        return false
      }
    }
    return true
  }

  async function upload(files) {
    if (!validate(files)) return null

    uploading.value = true
    progresses.value = {}

    try {
      const { data } = await itemApi.getPresignedUrls(
        files.map((f) => ({ contentType: f.type, fileSize: f.size }))
      )
      const presignedResults = data.data

      const uploads = presignedResults.map((result, i) =>
        axios.put(result.uploadUrl, files[i], {
          headers: { 'Content-Type': files[i].type },
          onUploadProgress: (e) => {
            progresses.value[i] = Math.round((e.loaded / e.total) * 100)
          },
        }).then(() => result.s3Key)
      )

      return await Promise.all(uploads)
    } finally {
      uploading.value = false
    }
  }

  return { uploading, progresses, upload }
}
