var ocode = {
    link: function (ele, elementid) {

        var area = typeof elementid === 'string' ? document.getElementById(elementid) : elementid[0];
        var getText = function () {
            return area.value.substr(area.selectionStart, (area.selectionEnd - area.selectionStart));
        };

        ele = $(ele);
        ele.selectable({
            url: '/tools',
            query: getText,
            select: function (id, title, item) {
                var text = getText();
                if (text === '') {
                    text = item.title;
                }
                var replace = (area.selectionEnd === area.selectionStart) ? item.title : text;
                ocode.insertText(area, '[' + item.tag + ' ' + replace + "]");
            }
        })
    },
    reversecase: function (element) {
        var ele = element[0];
        var selectionStart = ele.selectionStart;
        var selectionEnd = ele.selectionEnd;
        var sel = ele.value.substr(ele.selectionStart, (ele.selectionEnd - ele.selectionStart));
        if (sel.toLowerCase() === sel) {
            ocode.insertText(ele, sel.toUpperCase());
        } else {
            ocode.insertText(ele, sel.toLowerCase());
        }
        ele.setSelectionRange(selectionStart, selectionEnd);

    },
    uppercase: function (element) {
        var ele = element[0];
        var selectionStart = ele.selectionStart;
        var selectionEnd = ele.selectionEnd;
        var sel = ele.value.substr(ele.selectionStart, (ele.selectionEnd - ele.selectionStart));
        ocode.insertText(ele, sel.toUpperCase());
        ele.setSelectionRange(selectionStart, selectionEnd);
    },
    lowercase: function (element) {
        var ele = element[0];
        var selectionStart = ele.selectionStart;
        var selectionEnd = ele.selectionEnd;
        var sel = ele.value.substr(ele.selectionStart, (ele.selectionEnd - ele.selectionStart));
        ocode.insertText(ele, sel.toLowerCase());
        ele.setSelectionRange(selectionStart, selectionEnd);
    },
    tag: function (element, tagin, tagout) {
        var ele = element[0];
        var selectionStart = ele.selectionStart;
        var sel = ele.value.substr(ele.selectionStart, (ele.selectionEnd - ele.selectionStart));
        var text = tagin + sel + ((tagout === undefined) ? "" : tagout);
        ocode.insertText(ele, text);
        ele.setSelectionRange(tagin.length + selectionStart, tagin.length + selectionStart + sel.length);

    },
    tab: function (ele) {
        var selectionStart = ele.selectionStart;
        var sel = ele.value.substr(ele.selectionStart, (ele.selectionEnd - ele.selectionStart));
        var sels = sel.split("\n");
        sel = "";
        for (var i in sels) {
            sel += sels[i].length > 0 ? "\t" + sels[i] : "";
            if (i < sels.length - 1) {
                sel += "\n";
            }
        }
        ocode.insertText(ele, sel);
        ele.setSelectionRange(selectionStart, selectionStart + sel.length);
    },
    insertText: function (ele, text) {
        ele.focus();
        if (document.queryCommandSupported('insertText')) {
            document.execCommand("insertText", false, text);
        } else {
            var before = ele.value.substr(0, ele.selectionStart);
            var after = ele.value.substr(ele.selectionEnd, (ele.value.length - ele.selectionEnd));
            ele.value = before + text + after;
        }
    }
};