var jlab = jlab || {};
jlab.adm = jlab.adm || {};

jlab.adm.deploy = function () {
    let env = $("#env").val(),
    app = $("#app").val(),
    ver = $("#ver").val();

    $("#result-message").removeClass("success");
    $("#result-message").text("");

    let url = jlab.contextPath + "/deploy",
        data = {env: env, app: app, ver: ver};

        var promise = jlab.doAjaxJsonPostRequest(url, data, null, false);

        promise.done(function (data) {
            console.log('done', data);

            if(Object.hasOwn(data, 'exception')) {
                $("#result-message").removeClass("success");
                $("#result-message").text("Fail: " + data.exception);
            } else {
                $("#env").val("");
                $("#app").val("");
                $("#ver").val("");
                $("#result-message").addClass("success");
                $("#result-message").html('Success: Job submitted successfully. See asynchronous log for job <a href="/adm/log?jobId=' + data.jobId + '">#' + data.jobId + '</a>');
            }
        });

        promise.fail(function () {
            console.log('fail');
            $("#result-message").removeClass("success");
            $("#result-message").text("Fail: Unable to connect to adm");
        });
};

$(document).on("click", "#deploy-submit", function () {
    jlab.adm.deploy();

    return false;
});