var mqtt = require('mqtt')

var client  = mqtt.connect('ws://192.168.0.102:8888', {
  clientId: '',
  username: '',
  password: ''
})
 
client.on('connect', function () {
  const subscribeOptions = {
    qos: 2,
  }
  client.subscribe('gw8ynRwjXqKHYmkebLs5/receive_sms', subscribeOptions, function (err) {
    if (err) {
      console.error(err)
    }
  })
  client.subscribe('gw8ynRwjXqKHYmkebLs5/send-sms-results', subscribeOptions, function (err) {
    if (err) {
      console.error(err)
    }
  })
})

client.on('error', (error) => {
  console.error(error)
});
 
client.on('message', function (topic, payload, packet) {
  console.log(topic, '->', payload.toString(), packet.cmd, client.getLastMessageId())
})