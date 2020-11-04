var mqtt = require('mqtt')

var client  = mqtt.connect('tcp://localhost:1883', {
  clientId: '',
  username: '',
  password: ''
})
 
client.on('connect', function () {
  const subscribeOptions = {
    qos: 2,
  }
  client.subscribe('f8Ji049ES6RCY25yFPqq/receive_sms', subscribeOptions, function (err) {
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