---
title: About
description: A static site generator built with Java and Quarkus. Zero config to get started, full power of the JVM when you need it.
layout: page
---

# About this site

This site is built with [Roq](https://iamroq.dev), a static site generator powered by [Quarkus](https://quarkus.io). It combines the best of tools like Jekyll and Hugo with the Java ecosystem: zero configuration to get started, blazing fast live-reload in dev mode, and full access to Java when you need it.

## Authors

<div class="authors">
  {#for id in cdi:authors.fields}
    {#let author=cdi:authors.get(id)}
    {#roq/authorCard name=author.name avatar=author.avatar?? nickname=author.nickname profile=author.profile /}
  {/for}
</div>

