/* eslint-disable @typescript-eslint/no-unused-vars */
/**
 * MockJS工具对象
 */
const Mock = {
  /** MockJS包裹数据，支持mockjs语法 */
  mock (options = {}) {},
  /** 随机产生数据 */
  Random: {
    // Basic
    boolean: (min, max, current) => {},
    natural: () => {},
    integer: (min, max) => {},
    float: () => {},
    character: () => {},
    string: () => {},
    range: () => {},
    date: () => {},
    time: () => {},
    datetime: () => {},
    now: () => {},
    // Image
    image: () => {},
    dataImage: () => {},
    // Color
    color: () => {},
    // Text
    paragraph: () => {},
    sentence: () => {},
    word: () => {},
    title: () => {},
    cparagraph: () => {},
    csentence: () => {},
    cword: () => {},
    ctitle: () => {},
    // Name
    first: () => {},
    last: () => {},
    name: () => {},
    cfirst: () => {},
    clast: () => {},
    cname: () => {},
    // Web
    url: () => {},
    domain: () => {},
    email: () => {},
    ip: () => {},
    tld: () => {},
    // Address
    area: () => {},
    region: () => {},
    // Helper
    capitalize: () => {},
    upper: () => {},
    lower: () => {},
    pick: () => {},
    shuffle: () => {},
    // Miscellaneous
    guid: () => {},
    id: () => {}
  }
}

export const MockRandom = Mock.Random
