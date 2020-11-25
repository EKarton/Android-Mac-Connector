var mqtt = require('mqtt')

var client  = mqtt.connect('ws://192.168.0.102:3000', {
  clientId: '',
  username: '',
  password: ''
})
 
client.on('connect', function () {
  const deviceId = 'nwG5p0msxbiq0kzrpEy7'
  const publishOptions = {
    qos: 2,
    retain: true
  }
  console.log("Publishing message")

  // const sendSmsMessage = {
  //   "phone_number": "647-607-6358 647-666-9090 324-234-2344",
  //   "message": "Testing message",
  //   "message_id": "3",
  // }
  // client.publish(`${deviceId}/send-sms-request`, JSON.stringify(sendSmsMessage), publishOptions, (error, packet) => {
  //   if (error) {
  //     console.error(error)
  //   }
  //   client.end()
  // })

  // const queryThreads = {
  //   "limit": 10,
  //   "start": 0,
  // }
  // client.publish(`${deviceId}/sms/threads/query-requests`, JSON.stringify(queryThreads), publishOptions, (error, packet) => {
  //   if (error) {
  //     console.error(error)
  //   }
  //   client.end()
  // })

  // const queryMessages = {
  //   "limit": 10,
  //   "start": 0,
  //   "thread_id": 12,
  // }
  // client.publish(`${deviceId}/sms/messages/query-requests`, JSON.stringify(queryMessages), publishOptions, (error, packet) => {
  //   if (error) {
  //     console.error(error)
  //   }
  //   client.end()
  // })

  // const notificationResponse = {
  //   "key": "0|com.facebook.orca|10000|ONE_TO_ONE:100058025457291:100053450498376|10155",
  //   "action_type": "direct_reply_action",
  //   "action_title": "Reply",
  //   "action_reply_message": "This is a generated test message"
  // }
  const notificationResponse = {
    "key": "0|com.facebook.orca|10000|ONE_TO_ONE:100058025457291:100053450498376|10155",
    "action_type": "action_button",
    "action_title": "Like"
  }

  client.publish(`${deviceId}/notification/responses`, JSON.stringify(notificationResponse), publishOptions, (error, packet) => {
    if (error) {
      console.error(error)
    }
    client.end()
  })
})

client.on('error', (error) => {
  console.error(error)
});
