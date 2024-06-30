import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(utc)
dayjs.extend(duration)
dayjs.extend(timezone)
dayjs.tz.setDefault('Asia/Shanghai')

export default dayjs
