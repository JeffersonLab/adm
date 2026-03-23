var jlab = jlab || {};
jlab.editableRowTable = jlab.editableRowTable || {};
jlab.editableRowTable.entity = 'App Env';
jlab.editableRowTable.dialog.width = 650;
jlab.editableRowTable.dialog.height = 400;
jlab.addRow = function() {
    var appName = $("#row-app-name").val(),
        envName = $("#row-env-name").val(),
        requestUsername = $("#row-request-username").val(),
        deployUsername = $("#row-deploy-username").val(),
        deployHostname = $("#row-deploy-hostname").val(),
        deployPort = $("#row-deploy-port").val(),
        deployCommand = $("#row-deploy-command").val(),
        reloading = false;

    $(".dialog-submit-button")
        .height($(".dialog-submit-button").height())
        .width($(".dialog-submit-button").width())
        .empty().append('<div class="button-indicator"></div>');
    $(".dialog-close-button").attr("disabled", "disabled");
    $(".ui-dialog-titlebar button").attr("disabled", "disabled");

    var request = jQuery.ajax({
        url: jlab.contextPath + "/inventory/ajax/add-app-env",
        type: "POST",
        data: {
            appName: appName,
            envName: envName,
            requestUsername: requestUsername,
            deployUsername: deployUsername,
            deployHostname: deployHostname,
            deployPort: deployPort,
            deployCommand: deployCommand
        },
        dataType: "json"
    });

    request.done(function(json) {
        if (json.stat === 'ok') {
            reloading = true;
            window.location.reload();
        } else {
            alert(json.error);
        }
    });

    request.fail(function(xhr, textStatus) {
        window.console && console.log('Unable to add app env; Text Status: ' + textStatus + ', Ready State: ' + xhr.readyState + ', HTTP Status Code: ' + xhr.status);
        alert('Unable to Save: Server unavailable or unresponsive');
    });

    request.always(function() {
        if (!reloading) {
            $(".dialog-submit-button").empty().text("Save");
            $(".dialog-close-button").removeAttr("disabled");
            $(".ui-dialog-titlebar button").removeAttr("disabled");
        }
    });
};
jlab.editRow = function(removeSync) {
    var appEnvId = $(".editable-row-table tr.selected-row").attr("data-id"),
        appName = $("#row-app-name").val(),
        envName = $("#row-env-name").val(),
        requestUsername = $("#row-request-username").val(),
        deployUsername = $("#row-deploy-username").val(),
        deployHostname = $("#row-deploy-hostname").val(),
        deployPort = $("#row-deploy-port").val(),
        deployCommand = $("#row-deploy-command").val(),
        reloading = false;

    $(".dialog-submit-button")
        .height($(".dialog-submit-button").height())
        .width($(".dialog-submit-button").width())
        .empty().append('<div class="button-indicator"></div>');
    $(".dialog-close-button").attr("disabled", "disabled");
    $(".ui-dialog-titlebar button").attr("disabled", "disabled");

    var request = jQuery.ajax({
        url: jlab.contextPath + "/inventory/ajax/edit-app-env",
        type: "POST",
        data: {
            appEnvId: appEnvId,
            appName: appName,
            envName: envName,
            requestUsername: requestUsername,
            deployUsername: deployUsername,
            deployHostname: deployHostname,
            deployPort: deployPort,
            deployCommand: deployCommand
        },
        dataType: "json"
    });

    request.done(function(json) {
        if (json.stat === 'ok') {
            reloading = true;
            window.location.reload();
        } else {
            alert(json.error);
        }
    });

    request.fail(function(xhr, textStatus) {
        window.console && console.log('Unable to edit app env; Text Status: ' + textStatus + ', Ready State: ' + xhr.readyState + ', HTTP Status Code: ' + xhr.status);
        alert('Unable to Save: Server unavailable or unresponsive');
    });

    request.always(function() {
        if (!reloading) {
            $(".dialog-submit-button").empty().text("Save");
            $(".dialog-close-button").removeAttr("disabled");
            $(".ui-dialog-titlebar button").removeAttr("disabled");

        }
    });
};
jlab.removeRow = function() {
    var name = $(".editable-row-table tr.selected-row td:first-child").text(),
        id = $(".editable-row-table tr.selected-row").attr("data-id"),
        reloading = false;

    $("#remove-row-button")
        .height($("#remove-row-button").height())
        .width($("#remove-row-button").width())
        .empty().append('<div class="button-indicator"></div>');

    var request = jQuery.ajax({
        url: jlab.contextPath + "/inventory/ajax/remove-app-env",
        type: "POST",
        data: {
            appEnvId: id
        },
        dataType: "json"
    });

    request.done(function(json) {
        if (json.stat === 'ok') {
            reloading = true;
            window.location.reload();
        } else {
            alert(json.error);
        }
    });

    request.fail(function(xhr, textStatus) {
        window.console && console.log('Unable to remove app; Text Status: ' + textStatus + ', Ready State: ' + xhr.readyState + ', HTTP Status Code: ' + xhr.status);
        alert('Unable to Remove Server unavailable or unresponsive');
    });

    request.always(function() {
        if (!reloading) {
            $("#remove-row-button").empty().text("Remove");
        }
    });
};
$(document).on("dialogclose", "#table-row-dialog", function() {
    $("#row-form")[0].reset();

    $("#row-topics").val(null).trigger("change");

    $("#row-name").removeAttr("disabled");
    $("#row-repo").removeAttr("disabled");
});
$(document).on("click", "#open-edit-row-dialog-button", function() {
    var $selectedRow = $(".editable-row-table tr.selected-row");
    $("#row-app-name").val($selectedRow.attr("data-app-name"));
    $("#row-env-name").val($selectedRow.attr("data-env-name"));
    $("#row-request-username").val($selectedRow.attr("data-request-username"));
    $("#row-deploy-username").val($selectedRow.attr("data-deploy-username"));
    $("#row-deploy-hostname").val($selectedRow.attr("data-deploy-hostname"));
    $("#row-deploy-port").val($selectedRow.attr("data-deploy-port"));
    $("#row-deploy-command").val($selectedRow.attr("data-deploy-command"));
});
$(document).on("table-row-add", function() {
    jlab.addRow();
});
$(document).on("table-row-edit", function() {
    jlab.editRow();
});
$(document).on("click", "#remove-row-button", function() {
    var name = $(".editable-row-table tr.selected-row td:first-child").text().trim();
    if (confirm('Are you sure you want to remove ' + name + '?')) {
        jlab.removeRow();
    }
});
$(document).on("click", ".default-clear-panel", function () {
    $("#env-name").val('');
    $("#app-name").val('');
    $("#hostname").val('');
    return false;
});
$(function(){

});