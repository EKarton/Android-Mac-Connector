var mqtt = require('mqtt')

var client  = mqtt.connect('ws://192.168.0.102:8888', {
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
  const message = {
    "phone_number": "647-607-6358",
    "message": "Testing message",
    "message_id": "3",
  }
  client.publish('1234/testing', JSON.stringify(message), publishOptions, (error, packet) => {
    if (error) {
      console.error(error)
    }
    client.end()
  })
})

client.on('error', (error) => {
  console.error(error)
});
