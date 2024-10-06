//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.30.15"
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.30.15"

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

case class PublicRepos(public_repos: Int)

case class Repo(name: String)

case class Contributor(login: String, contributions: Long)

implicit val publicReposCodec: JsonValueCodec[PublicRepos] = JsonCodecMaker.make
implicit val repoCodec: JsonValueCodec[Repo] = JsonCodecMaker.make
implicit val contributorCodec: JsonValueCodec[Contributor] = JsonCodecMaker.make

println(readFromString[PublicRepos](
"""
{
  "login": "typelevel",
  "id": 3731824,
  "node_id": "MDEyOk9yZ2FuaXphdGlvbjM3MzE4MjQ=",
  "url": "https://api.github.com/orgs/typelevel",
  "repos_url": "https://api.github.com/orgs/typelevel/repos",
  "events_url": "https://api.github.com/orgs/typelevel/events",
  "hooks_url": "https://api.github.com/orgs/typelevel/hooks",
  "issues_url": "https://api.github.com/orgs/typelevel/issues",
  "members_url": "https://api.github.com/orgs/typelevel/members{/member}",
  "public_members_url": "https://api.github.com/orgs/typelevel/public_members{/member}",
  "avatar_url": "https://avatars.githubusercontent.com/u/3731824?v=4",
  "description": "Let the Scala compiler work for you.",
  "name": "typelevel.scala",
  "company": null,
  "blog": "http://typelevel.org",
  "location": null,
  "email": "info@typelevel.org",
  "twitter_username": "typelevel",
  "is_verified": true,
  "has_organization_projects": true,
  "has_repository_projects": true,
  "public_repos": 101,
  "public_gists": 0,
  "followers": 239,
  "following": 0,
  "html_url": "https://github.com/typelevel",
  "created_at": "2013-02-28T22:02:46Z",
  "updated_at": "2023-02-04T20:30:48Z",
  "archived_at": null,
  "type": "Organization"
}
"""
).public_repos)

println(readFromString[Repo](
"""
{
  "id": 1804055,
  "node_id": "MDEwOlJlcG9zaXRvcnkxODA0MDU1",
  "name": "scalacheck",
  "full_name": "typelevel/scalacheck",
  "private": false,
  "owner": {
    "login": "typelevel",
    "id": 3731824,
    "node_id": "MDEyOk9yZ2FuaXphdGlvbjM3MzE4MjQ=",
    "avatar_url": "https://avatars.githubusercontent.com/u/3731824?v=4",
    "gravatar_id": "",
    "url": "https://api.github.com/users/typelevel",
    "html_url": "https://github.com/typelevel",
    "followers_url": "https://api.github.com/users/typelevel/followers",
    "following_url": "https://api.github.com/users/typelevel/following{/other_user}",
    "gists_url": "https://api.github.com/users/typelevel/gists{/gist_id}",
    "starred_url": "https://api.github.com/users/typelevel/starred{/owner}{/repo}",
    "subscriptions_url": "https://api.github.com/users/typelevel/subscriptions",
    "organizations_url": "https://api.github.com/users/typelevel/orgs",
    "repos_url": "https://api.github.com/users/typelevel/repos",
    "events_url": "https://api.github.com/users/typelevel/events{/privacy}",
    "received_events_url": "https://api.github.com/users/typelevel/received_events",
    "type": "Organization",
    "site_admin": false
  },
  "html_url": "https://github.com/typelevel/scalacheck",
  "description": "Property-based testing for Scala",
  "fork": false,
  "url": "https://api.github.com/repos/typelevel/scalacheck",
  "forks_url": "https://api.github.com/repos/typelevel/scalacheck/forks",
  "keys_url": "https://api.github.com/repos/typelevel/scalacheck/keys{/key_id}",
  "collaborators_url": "https://api.github.com/repos/typelevel/scalacheck/collaborators{/collaborator}",
  "teams_url": "https://api.github.com/repos/typelevel/scalacheck/teams",
  "hooks_url": "https://api.github.com/repos/typelevel/scalacheck/hooks",
  "issue_events_url": "https://api.github.com/repos/typelevel/scalacheck/issues/events{/number}",
  "events_url": "https://api.github.com/repos/typelevel/scalacheck/events",
  "assignees_url": "https://api.github.com/repos/typelevel/scalacheck/assignees{/user}",
  "branches_url": "https://api.github.com/repos/typelevel/scalacheck/branches{/branch}",
  "tags_url": "https://api.github.com/repos/typelevel/scalacheck/tags",
  "blobs_url": "https://api.github.com/repos/typelevel/scalacheck/git/blobs{/sha}",
  "git_tags_url": "https://api.github.com/repos/typelevel/scalacheck/git/tags{/sha}",
  "git_refs_url": "https://api.github.com/repos/typelevel/scalacheck/git/refs{/sha}",
  "trees_url": "https://api.github.com/repos/typelevel/scalacheck/git/trees{/sha}",
  "statuses_url": "https://api.github.com/repos/typelevel/scalacheck/statuses/{sha}",
  "languages_url": "https://api.github.com/repos/typelevel/scalacheck/languages",
  "stargazers_url": "https://api.github.com/repos/typelevel/scalacheck/stargazers",
  "contributors_url": "https://api.github.com/repos/typelevel/scalacheck/contributors",
  "subscribers_url": "https://api.github.com/repos/typelevel/scalacheck/subscribers",
  "subscription_url": "https://api.github.com/repos/typelevel/scalacheck/subscription",
  "commits_url": "https://api.github.com/repos/typelevel/scalacheck/commits{/sha}",
  "git_commits_url": "https://api.github.com/repos/typelevel/scalacheck/git/commits{/sha}",
  "comments_url": "https://api.github.com/repos/typelevel/scalacheck/comments{/number}",
  "issue_comment_url": "https://api.github.com/repos/typelevel/scalacheck/issues/comments{/number}",
  "contents_url": "https://api.github.com/repos/typelevel/scalacheck/contents/{+path}",
  "compare_url": "https://api.github.com/repos/typelevel/scalacheck/compare/{base}...{head}",
  "merges_url": "https://api.github.com/repos/typelevel/scalacheck/merges",
  "archive_url": "https://api.github.com/repos/typelevel/scalacheck/{archive_format}{/ref}",
  "downloads_url": "https://api.github.com/repos/typelevel/scalacheck/downloads",
  "issues_url": "https://api.github.com/repos/typelevel/scalacheck/issues{/number}",
  "pulls_url": "https://api.github.com/repos/typelevel/scalacheck/pulls{/number}",
  "milestones_url": "https://api.github.com/repos/typelevel/scalacheck/milestones{/number}",
  "notifications_url": "https://api.github.com/repos/typelevel/scalacheck/notifications{?since,all,participating}",
  "labels_url": "https://api.github.com/repos/typelevel/scalacheck/labels{/name}",
  "releases_url": "https://api.github.com/repos/typelevel/scalacheck/releases{/id}",
  "deployments_url": "https://api.github.com/repos/typelevel/scalacheck/deployments",
  "created_at": "2011-05-26T11:44:12Z",
  "updated_at": "2023-11-25T04:21:54Z",
  "pushed_at": "2023-11-22T04:07:41Z",
  "git_url": "git://github.com/typelevel/scalacheck.git",
  "ssh_url": "git@github.com:typelevel/scalacheck.git",
  "clone_url": "https://github.com/typelevel/scalacheck.git",
  "svn_url": "https://github.com/typelevel/scalacheck",
  "homepage": "http://www.scalacheck.org",
  "size": 71447,
  "stargazers_count": 1907,
  "watchers_count": 1907,
  "language": "Scala",
  "has_issues": true,
  "has_projects": false,
  "has_downloads": true,
  "has_wiki": false,
  "has_pages": true,
  "has_discussions": false,
  "forks_count": 408,
  "mirror_url": null,
  "archived": false,
  "disabled": false,
  "open_issues_count": 65,
  "license": {
    "key": "bsd-3-clause",
    "name": "BSD 3-Clause \"New\" or \"Revised\" License",
    "spdx_id": "BSD-3-Clause",
    "url": "https://api.github.com/licenses/bsd-3-clause",
    "node_id": "MDc6TGljZW5zZTU="
  },
  "allow_forking": true,
  "is_template": false,
  "web_commit_signoff_required": false,
  "topics": [
    "property-testing",
    "scala",
    "scalacheck"
  ],
  "visibility": "public",
  "forks": 408,
  "open_issues": 65,
  "watchers": 1907,
  "default_branch": "main",
  "permissions": {
    "admin": false,
    "maintain": false,
    "push": false,
    "triage": false,
    "pull": true
  }
}
"""
).name)

println(readFromString[Contributor](
"""
{
  "login": "ceedubs",
  "id": 977929,
  "node_id": "MDQ6VXNlcjk3NzkyOQ==",
  "avatar_url": "https://avatars.githubusercontent.com/u/977929?v=4",
  "gravatar_id": "",
  "url": "https://api.github.com/users/ceedubs",
  "html_url": "https://github.com/ceedubs",
  "followers_url": "https://api.github.com/users/ceedubs/followers",
  "following_url": "https://api.github.com/users/ceedubs/following{/other_user}",
  "gists_url": "https://api.github.com/users/ceedubs/gists{/gist_id}",
  "starred_url": "https://api.github.com/users/ceedubs/starred{/owner}{/repo}",
  "subscriptions_url": "https://api.github.com/users/ceedubs/subscriptions",
  "organizations_url": "https://api.github.com/users/ceedubs/orgs",
  "repos_url": "https://api.github.com/users/ceedubs/repos",
  "events_url": "https://api.github.com/users/ceedubs/events{/privacy}",
  "received_events_url": "https://api.github.com/users/ceedubs/received_events",
  "type": "User",
  "site_admin": false,
  "contributions": 641
}
"""
))

case class Contributions(count: Long, contributors: Vector[Contributor])

implicit val contributionsCodec: JsonValueCodec[Contributions] = JsonCodecMaker.make

val contributions = Contributions(2, Vector(
  Contributor("Ghurtchu", 1),
  Contributor("plokhotnyuk", 1)
))

println(writeToString(contributions))

println(writeToString(contributions, WriterConfig.withIndentionStep(2)))
