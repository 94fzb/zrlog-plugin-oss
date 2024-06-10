$(function () {
    const e = JSON.parse(document.getElementById("data").innerText);

    $("#syncTemplate-switch").bootstrapSwitch('state', e.syncTemplate === 'on');
    $("#syncTemplateVal").attr("value", e.syncTemplate);
    $("#supportHttps-switch").bootstrapSwitch('state', e.supportHttps === 'on');
    $("#supportHttpsVal").attr("value", e.supportHttps);
    $("#syncHtml-switch").bootstrapSwitch('state', e.syncHtml === 'on');
    $("#syncHtmlVal").attr("value", e.syncHtml);

    new Vue({
        el: '#vue-div',
        data: {
            oss: e
        },
        methods: {
            val: function (val) {
                return val;
            }
        }
    });

    $('#syncTemplate-switch').on('switchChange.bootstrapSwitch', function (event, state) {
        $("#syncTemplateVal").attr("value", state ? "on" : "off");
    });
    $('#supportHttps-switch').on('switchChange.bootstrapSwitch', function (event, state) {
        $("#supportHttpsVal").attr("value", state ? "on" : "off");
    });
    $('#syncHtml-switch').on('switchChange.bootstrapSwitch', function (event, state) {
        $("#syncHtmlVal").attr("value", state ? "on" : "off");
    });

    $(".btn-info").click(function () {
        const formId = "ajax" + $(this).attr("id");
        $.post('update', $("#" + formId).serialize(), function (data) {
            if (data.success || data.status === 200) {
                $.gritter.add({
                    title: '  操作成功...',
                    class_name: 'gritter-success' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : ''),
                });
            } else {
                $.gritter.add({
                    title: '  发生了一些异常...',
                    class_name: 'gritter-error' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : ''),
                });
            }
        });
    });
});