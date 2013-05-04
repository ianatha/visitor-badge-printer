var buf = [];
var last_timestamp = 0;
var maybe_char = false;
var enabled = true;

function simulateKeyPress(character) {
  jQuery.event.trigger({ type : 'keypress', which : character.charCodeAt(0) });
}

function fastInput(s) {
    console.log(s);
}

function keycodesToString(k) {
    return $.map(k, function (val, i) {
        return String.fromCharCode(val);
    }).join("");
}

$(function() {
    $.fn.vAlign = function (t) {
        return this.each(function (n) {
            var r, i, s;
            return r = $(this).height(), s = $(this).parent().height(), i = Math.ceil((s - r) * t), $(this).css("padding-top", i)
        })
    };

    $(document).ready(function () {

        $("body").keypress(function (e) {
            if (!enabled) {
                return;
            }

            var key = e.which;
            var when = e.timeStamp;

            var delta = when - last_timestamp;
            if (delta <= 40) {
                event.preventDefault();
                if (maybe_char != false) {
                    buf.push(maybe_char);
                    maybe_char = false;
                }
            } else {
                if (maybe_char != false) {
                    console.log("not maybe but can't type it");
                    maybe_char = false;
                }
            }


            if (key != 13) {
                buf.push(key);
            } else {
                var s = keycodesToString(buf);
                buf = []
                fastInput(s);
            }

            if (delta > 500) {
                buf = [];
                if (key == 37) {
                    console.log("maybe");
                    maybe_char = key;
                    event.preventDefault();
                }
            }
            last_timestamp = when;
        });

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