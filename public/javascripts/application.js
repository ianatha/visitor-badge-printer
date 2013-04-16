(function () {

}).call(this), function () {
}.call(this), function () {
    var e;
    e = jQuery, e.fn.vAlign = function (t) {
        return this.each(function (n) {
            var r, i, s;
            return r = e(this).height(), s = e(this).parent().height(), i = Math.ceil((s - r) * t), e(this).css("padding-top", i)
        })
    }, e(document).ready(function () {
        e("#splashButtons").vAlign(.43), e("#nda-agree").live("click tap", function (t) {
            return e(".nda-accepted").value(1)
        }), e(document).bind("mobileinit", function () {
            return e.extend(e.mobile, {metaViewportContent: "width=device-width, height=device-height, minimum-scale=1, maximum-scale=1"})
        }), e(".deleteButton").click(function () {
            var t, n;
            return t = e(event.target).closest("a"), n = t.attr("data-person-id"), e.get("/signout", {first_name: t.attr("data-first"), last_name: t.attr("data-last")}, function (e) {
                return window.location.reload()
            }), !1
        })
    })
}.call(this);