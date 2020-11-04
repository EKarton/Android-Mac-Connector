var mqtt = require('mqtt')

var client  = mqtt.connect('tcp://localhost:1883', {
  clientId: '',
  username: '',
  password: ''
})
 
client.on('connect', function () {
  const publishOptions = {
    qos: 2,
    retain: true
  }
  console.log("Publishing message")
  client.publish('f8Ji049ES6RCY25yFPqq/receive_sms', 'From node js: testing', publishOptions, (error, packet) => {
    if (error) {
      console.error(error)
    }
    client.end()
  })
})

client.on('error', (error) => {
  console.error(error)
});
