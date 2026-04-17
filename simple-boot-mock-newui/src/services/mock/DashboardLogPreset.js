import dayjs from 'dayjs'

export const DASHBOARD_LOG_NAME = 'MockController#doMock'

export const DashboardLogPreset = {
  TODAY_CALLS: 'todayCalls',
  TOTAL_CALLS: 'totalCalls'
}

export const DashboardLogScope = {
  ALL: 'all',
  MINE: 'mine'
}

const normalizeQueryValue = (value) => Array.isArray(value) ? value[0] : value

const buildTodayDateRange = () => {
  return [dayjs().startOf('day').toDate(), dayjs().endOf('day').toDate()]
}

export const buildDashboardLogRoute = (preset, all = false) => {
  return {
    name: 'MockLogs',
    query: {
      preset,
      scope: all ? DashboardLogScope.ALL : DashboardLogScope.MINE
    }
  }
}

export const resolveDashboardLogPreset = (query = {}, currentUserName) => {
  const preset = normalizeQueryValue(query.preset)
  const scope = normalizeQueryValue(query.scope) === DashboardLogScope.ALL
    ? DashboardLogScope.ALL
    : DashboardLogScope.MINE
  const matched = Object.values(DashboardLogPreset).includes(preset)
  if (!matched) {
    return {
      matched: false,
      dateRange: [],
      searchParam: {}
    }
  }
  const searchParam = {
    logName: DASHBOARD_LOG_NAME
  }
  if (scope === DashboardLogScope.MINE && currentUserName) {
    searchParam.userName = currentUserName
  }
  return {
    matched: true,
    dateRange: preset === DashboardLogPreset.TODAY_CALLS ? buildTodayDateRange() : [],
    searchParam
  }
}
