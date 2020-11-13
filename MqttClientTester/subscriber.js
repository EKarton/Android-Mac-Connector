var mqtt = require('mqtt')

var client  = mqtt.connect('ws://192.168.0.102:8888', {
  clientId: '',
  username: '',
  password: ''
})
 
client.on('connect', function () {
  const deviceId = 'WRF2KV4cefu0HuH8LDks'
  const subscribeOptions = {
    qos: 2,
  }
  client.subscribe(`${deviceId}/sms/threads/query-results`, subscribeOptions, function (err) {
    if (err) {
      console.error(err)
    }
  })

  client.subscribe(`${deviceId}/sms/messages/query-results`, subscribeOptions, function (err) {
    if (err) {
      console.error(err)
    }
  })

  client.subscribe(`${deviceId}/send-sms-results`, subscribeOptions, function (err) {
    if (err) {
      console.error(err)
    }
  })
})

client.on('error', (error) => {
  console.error(error)
});
 
client.on('message', function (topic, payload, packet) {
  console.log(topic, '->', payload.toString(), packet.cmd, client.getLastMessageId(), '\n')
})