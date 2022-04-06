$(function () {
  var localusers = [];

  $('#name').focus();
  $('#adduserform').submit(function () {
    var name = $('#name').val();
    $('#name').val('');

    var socket = new WebSocket("ws://localhost:8881/ws/chat?name=" + name);

    socket.onopen = function (msg) {
      console.log("已连接成功！");
      console.log("onopen" + msg);
    }
    socket.onmessage = function (evt) {
      console.log("数据已接收..." + evt);
      var received_msg = evt.data;
      var info = JSON.parse(received_msg);
      if (info.type === 'allUser') {
        emptyUsers();
        info.users.forEach(function (value, index) {
          addUser(value, name);
        });
      } else if (info.type === 'chat') {
        sendMessage(info.from, info.msg, name);
      }
      $("#msg").append(info.msg + '<br>');
      console.log("数据已接收..." + received_msg);
    };

    socket.onclose = function () {
      // 关闭 websocket
      console.log("连接已关闭...");
    };

    $('.model').addClass('hidden');

    $('#message').focus();

    $('#sendmessage').on('click', function (event) {
      event.preventDefault();

      var message = $('#message').val();

      if (message.trim() !== '') {
        socket.send(message);
      }
      $('#message').val('');
      $('#message').focus();
    });

    return false;
  });

  function emptyUsers() {
    $('.user-list').empty();
  }

  function addUser(name, selfname) {
    var result = name === selfname ? "user " + " self" : "user ";
    $('.user-list').append($('<div></div>').addClass(result)
    .html('<img src="/ws/imgs/default.jpg" alt="user img">' +
        '<p class="username">' + name + '</p>'));
  }

  function sendMessage(from, msg, selfname) {
    var element = document.createElement('div');
    element.classList.add('info');

    if (selfname === from) {
      element.classList.add('myself');
    }

    element.innerHTML = '<p class="author">' + filterStr(from) +
        '</p><div class="message">' + filterStr(msg) + '</div>';
    $('.content-area').append(element);
    element.scrollIntoView(false);
  }

  //filter xss attack
  function filterStr(str) {
    var pattern = new RegExp(
        "[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？%+_]");
    var specialStr = "";
    for (var i = 0; i < str.length; i++) {
      specialStr += str.substr(i, 1).replace(pattern, '');
    }
    return specialStr;
  }
});