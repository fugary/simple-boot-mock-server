/* eslint-disable @typescript-eslint/no-unused-vars */
// 与Mock.Random属性对应的数据
export const MockRandom = {
  boolean: {
    func: (min, max, current) => {},
    desc: '返回一个随机的布尔值'
  },
  natural: {
    func: (min, max) => {},
    desc: '返回一个随机的自然数'
  },
  integer: {
    func: (min, max) => {},
    desc: '返回一个随机的整数'
  },
  float: {
    func: (min, max, dmin, dmax) => {},
    desc: '返回一个随机的浮点数'
  },
  character: {
    func: (pool) => {},
    desc: '返回一个随机字符'
  },
  string: {
    func: (pool, min, max) => {},
    desc: '返回一个随机字符串'
  },
  range: {
    func: (start, stop, step) => {},
    desc: '返回一个整型数组'
  },
  date: {
    func: (format) => {},
    desc: '返回一个随机的日期字符串'
  },
  time: {
    func: (format) => {},
    desc: '返回一个随机的时间字符串'
  },
  datetime: {
    func: (format) => {},
    desc: '返回一个随机的日期和时间字符串'
  },
  now: {
    func: (util, format) => {},
    desc: '返回当前的日期和时间字符串'
  },
  image: {
    func: (size, background, foreground, format, text) => {},
    desc: '返回一个随机的图片地址'
  },
  dataImage: {
    func: (size, text) => {},
    desc: '返回一段随机的 Base64 图片编码'
  },
  color: {
    func: () => {},
    desc: '返回一个有吸引力的颜色'
  },
  paragraph: {
    func: (min, max) => {},
    desc: '返回一段文本'
  },
  sentence: {
    func: (min, max) => {},
    desc: '返回一个句子'
  },
  word: {
    func: (min, max) => {},
    desc: '返回一个单词'
  },
  title: {
    func: (min, max) => {},
    desc: '返回一句标题'
  },
  cparagraph: {
    func: (min, max) => {},
    desc: '返回一段中文文本'
  },
  csentence: {
    func: (min, max) => {},
    desc: '返回一个中文句子'
  },
  cword: {
    func: (pool, min, max) => {},
    desc: '返回一个随机汉字字符串'
  },
  ctitle: {
    func: (min, max) => {},
    desc: '返回一句中文标题'
  },
  first: {
    func: () => {},
    desc: '返回一个常见的英文名'
  },
  last: {
    func: () => {},
    desc: '返回一个常见的英文姓'
  },
  name: {
    func: (middle) => {},
    desc: '返回一个常见的英文姓名'
  },
  cfirst: {
    func: () => {},
    desc: '返回一个常见的中文名'
  },
  clast: {
    func: () => {},
    desc: '返回一个常见的中文姓'
  },
  cname: {
    func: () => {},
    desc: '返回一个常见的中文姓名'
  },
  url: {
    func: (protocol, host) => {},
    desc: '返回一个随机的 URL'
  },
  domain: {
    func: () => {},
    desc: '返回一个随机的域名'
  },
  email: {
    func: (domain) => {},
    desc: '返回一个随机的邮件地址'
  },
  ip: {
    func: () => {},
    desc: '返回一个随机的 IP 地址'
  },
  tld: {
    func: () => {},
    desc: '返回一个顶级域名'
  },
  area: {
    func: () => {},
    desc: '返回一个（中国）大区'
  },
  region: {
    func: () => {},
    desc: '返回一个（中国）省（或直辖市、自治区、特别行政区）'
  },
  capitalize: {
    func: (word) => {},
    desc: '把字符串的第一个字母转换为大写'
  },
  upper: {
    func: (str) => {},
    desc: '把字符串转换为大写'
  },
  lower: {
    func: (str) => {},
    desc: '把字符串转换为小写'
  },
  pick: {
    func: (arr, min, max) => {},
    desc: '从数组中随机选取一个元素'
  },
  shuffle: {
    func: (arr, min, max) => {},
    desc: '打乱数组中元素的顺序，并返回'
  },
  guid: {
    func: () => {},
    desc: '返回一个随机的 GUID'
  },
  id: {
    func: () => {},
    desc: '返回一个 18 位身份证号'
  }
}
