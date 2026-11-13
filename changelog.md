# Changelog for v0.2.3

<!--
  Release notes for the next release of ScoreKeeper. Write Markdown below this comment.
  Everything under the header (HTML comments excluded) becomes the GitHub release and
  Modrinth changelog; the release version itself comes from the tag.
  After a release, reset this file for the next version and tag that commit
  v<next>-dev (see CONTRIBUTING.md).
-->

## Changed

- Now targets Paper 26.2 (Paper API 26.2, `api-version: '26.2'`) and runs on Paper 26.2 and 26.3 with Java 25. Servers on Paper 1.21.x are no longer supported by this version.
- Fixed the nullability (`@NonNull`) warnings in the plugin and command classes by checking command registration and message arguments explicitly.

## Development

- Source is now formatted with Spotless (google-java-format 1.36.1) and carries a license header.
- Added GitHub Actions: CI builds and tests every push and pull request to `main` and `develop`, uploads the jar and test reports, and creates a draft GitHub release when a `v*` tag is pushed.
- Release versions now come from `v*` Git tags (`tools/compute-version.sh`), and release notes come from this file.
- Documentation was reorganized and expanded.
