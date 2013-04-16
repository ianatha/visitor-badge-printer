$(function() {
    $.fn.vAlign = function (t) {
        return this.each(function (n) {
            var r, i, s;
            return r = $(this).height(), s = $(this).parent().height(), i = Math.ceil((s - r) * t), $(this).css("padding-top", i)
        })
    };

    $(document).ready(function () {
        $("#splashButtons").vAlign(.43);

        $("#nda-agree").live("click tap", function(t) {
            return $(".nda-accepted").value(1);
        });

        $(document).bind("mobileinit", function () {
            return $.extend($.mobile, {
                metaViewportContent: "width=device-width, height=device-height, minimum-scale=1, maximum-scale=1"
            });
        });

        $(".deleteButton").click(function (e) {
            console.log(e);
            console.log(event)
            var id = $(event.target).closest("a").attr("data-id");
            $.post("/api/1/signout/" + id, {}, function(e) {
                return window.location.reload();
            });
        })
    })
});