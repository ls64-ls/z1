export function formatDate(date: Date | string, fmt = 'yyyy-MM-dd'): string {
  const d = new Date(date)
  const o: Record<string, number> = {
    'M+': d.getMonth() + 1,
    'd+': d.getDate(),
    'h+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds()
  }
  let result = fmt
  if (/(y+)/.test(result)) {
    result = result.replace(RegExp.$1, String(d.getFullYear()).substring(4 - RegExp.$1.length))
  }
  for (const k in o) {
    if (new RegExp(`(${k})`).test(result)) {
      result = result.replace(RegExp.$1, RegExp.$1.length === 1 ? String(o[k]) : `00${o[k]}`.substring(String(o[k]).length))
    }
  }
  return result
}

export function getDateRange(start: Date, end: Date): Date[] {
  const dates: Date[] = []
  let current = new Date(start)
  while (current <= end) {
    dates.push(new Date(current))
    current.setDate(current.getDate() + 1)
  }
  return dates
}
