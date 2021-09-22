package yamusca

package object benchmark {

  // this test template is from mustache.java: https://github.com/spullara/mustache.java
  // Apache License 2.0
  val template =
    """<div>Timeline</div>
      |<div>
      |{{#tweets}}
      |<div>
      |<div>
      |{{#user}}
      |    <div><a href="https://twitter.com/{{screen_name}}">{{name}}</a> tweeted:</div>
      |    <div><img src="{{profile_image_url_https}}"></div>
      |    {{#entities}}
      |        {{#url}}
      |        <div><a href="{{url}}">{{display_url}}</a></div>
      |        {{/url}}
      |        {{#hashtags}}
      |         <div><a href="search?q={{tag}}">#{{tag}}</a></div>
      |        {{/hashtags}}
      |        {{#media}}
      |        <div><img src="{{media_url_https}}"></div>
      |        {{/media}}
      |        {{#user_mentions}}
      |        <div><a href="{{screen_name}}">{{name}}</a></div>
      |        {{/user_mentions}}
      |    {{/entities}}
      |{{/user}}
      |<div>{{text}}</div>
      |{{#entities}}
      |    {{#url}}
      |    <div><a href="{{url}}">{{display_url}}</a></div>
      |    {{/url}}
      |    {{#hashtags}}
      |    <div><a href="search?q={{tag}}">#{{tag}}</a></div>
      |    {{/hashtags}}
      |    {{#media}}
      |    <div><img src="{{media_url_https}}"></div>
      |    {{/media}}
      |    {{#user_mentions}}
      |    <div><a href="{{screen_name}}">{{name}}</a></div>
      |    {{/user_mentions}}
      |{{/entities}}
      |<div>{{{source}}}</div>
      |</div>
      |</div>
      |{{/tweets}}
      |</div>""".stripMargin

  val dataJson =
    """
{
  "in_reply_to_status_id": null,
  "possibly_sensitive": false,
  "in_reply_to_user_id_str": null,
  "contributors": null,
  "truncated": false,
  "id_str": "114176016611684353",
  "user": {
    "statuses_count": 5327,
    "profile_use_background_image": false,
    "time_zone": "Pacific Time (US & Canada)",
    "protected": false,
    "default_profile": false,
    "notifications": false,
    "profile_text_color": "333333",
    "name": "Richard Henry",
    "expanded_url": null,
    "default_profile_image": false,
    "following": true,
    "verified": false,
    "geo_enabled": true,
    "profile_background_image_url": "http:\/\/a0.twimg.com\/images\/themes\/theme1\/bg.png",
    "favourites_count": 98,
    "id_str": "31393",
    "utc_offset": -28800,
    "profile_link_color": "0084B4",
    "profile_image_url": "http:\/\/a3.twimg.com\/profile_images\/1192563998\/Photo_on_2010-02-22_at_23.32_normal.jpeg",
    "description": "Husband to @chelsea. Designer at @twitter. English expat living in California.",
    "is_translator": false,
    "profile_background_image_url_https": "https:\/\/si0.twimg.com\/images\/themes\/theme1\/bg.png",
    "location": "San Francisco, CA",
    "follow_request_sent": false,
    "friends_count": 184,
    "profile_background_color": "404040",
    "profile_background_tile": false,
    "url": null,
    "display_url": null,
    "profile_sidebar_fill_color": "DDEEF6",
    "followers_count": 2895,
    "profile_image_url_https": "https:\/\/si0.twimg.com\/profile_images\/1192563998\/Photo_on_2010-02-22_at_23.32_normal.jpeg",
    "entities": {
      "urls": [

      ],
      "hashtags": [

      ],
      "user_mentions": [
        {
          "name": "Chelsea Henry",
          "id_str": "16447200",
          "indices": [
            11,
            19
          ],
          "id": 16447200,
          "screen_name": "chelsea"
        },
        {
          "name": "Twitter",
          "id_str": "783214",
          "indices": [
            33,
            41
          ],
          "id": 783214,
          "screen_name": "twitter"
        }
      ]
    },
    "lang": "en",
    "show_all_inline_media": true,
    "listed_count": 144,
    "contributors_enabled": false,
    "profile_sidebar_border_color": "C0DEED",
    "id": 31393,
    "created_at": "Wed Nov 29 22:40:31 +0000 2006",
    "screen_name": "richardhenry"
  },
  "retweet_count": 0,
  "in_reply_to_user_id": null,
  "favorited": false,
  "geo": null,
  "in_reply_to_screen_name": null,
  "entities": {
    "urls": [

    ],
    "hashtags": [

    ],
    "media": [
      {
        "type": "photo",
        "expanded_url": "http:\/\/twitter.com\/richardhenry\/status\/114176016611684353\/photo\/1",
        "id_str": "114176016615878656",
        "indices": [
          22,
          42
        ],
        "url": "http:\/\/t.co\/wJxLOTOR",
        "media_url": "http:\/\/p.twimg.com\/AZWie3BCMAAcQJj.jpg",
        "display_url": "pic.twitter.com\/wJxLOTOR",
        "id": 1.1417601661588e+17,
        "media_url_https": "https:\/\/p.twimg.com\/AZWie3BCMAAcQJj.jpg",
        "sizes": {
          "small": {
            "h": 254,
            "w": 340,
            "resize": "fit"
          },
          "large": {
            "h": 765,
            "w": 1024,
            "resize": "fit"
          },
          "thumb": {
            "h": 150,
            "w": 150,
            "resize": "crop"
          },
          "medium": {
            "h": 448,
            "w": 600,
            "resize": "fit"
          }
        }
      }
    ],
    "user_mentions": [

    ]
  },
  "coordinates": null,
  "source": "<a href=\"http:\/\/twitter.com\" rel=\"nofollow\">Twitter for  iPhone<\/a>",
  "place": null,
  "retweeted": false,
  "id": 1.1417601661168e+17,
  "in_reply_to_status_id_str": null,
  "annotations": null,
  "text": "LOOK AT WHAT ARRIVED. http:\/\/t.co\/wJxLOTOR",
  "created_at": "Thu Sep 15 03:17:38 +0000 2011"
}
  """
}
