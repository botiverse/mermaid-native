import { defineConfig } from 'vitepress'

const base = process.env.DOCS_BASE ?? '/'

export default defineConfig({
  lang: 'en-US',
  title: 'Mermaid Native',
  description: 'Typed Mermaid-compatible parsing and native rendering with Kotlin Multiplatform.',
  base,
  cleanUrls: true,
  lastUpdated: true,
  appearance: true,
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'Mermaid Native',
    nav: [
      { text: 'Guide', link: '/guide/compatibility' },
      { text: 'Architecture', link: '/guide/architecture' },
      { text: 'Examples', link: '/examples' },
      { text: 'GitHub', link: 'https://github.com/botiverse/mermaid-native' },
    ],
    sidebar: {
      '/guide/': [
        {
          text: 'Guide',
          items: [
            { text: 'Getting started', link: '/guide/getting-started' },
            { text: 'Compatibility', link: '/guide/compatibility' },
            { text: 'Architecture', link: '/guide/architecture' },
            { text: 'Testing', link: '/guide/testing' },
          ],
        },
      ],
      '/reference/': [
        {
          text: 'Reference',
          items: [
            { text: 'Diagram family matrix', link: '/reference/families' },
            { text: 'Official family registry', link: '/reference/official-family-registry' },
          ],
        },
      ],
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/botiverse/mermaid-native' },
    ],
    search: { provider: 'local' },
    editLink: {
      pattern: 'https://github.com/botiverse/mermaid-native/edit/main/docs-site/:path',
      text: 'Edit this page on GitHub',
    },
    footer: {
      message: 'Mermaid-compatible, not Mermaid official.',
      copyright: 'Copyright © 2026 Mermaid Native contributors',
    },
  },
})
