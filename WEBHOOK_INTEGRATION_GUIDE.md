# Webhook Integration Guide: Standard Bank MyUpdates

This guide shows you how to set up automatic forwarding of Standard Bank MyUpdates email notifications to your ECD Payment Reconciliation System webhook.

---

## Overview

When a parent makes a payment, Standard Bank sends a MyUpdates email notification. We need to forward this email to our webhook endpoint so the system can automatically process the payment.

**Flow:**
```
Parent pays → Standard Bank sends email →
Email forwarding service captures email →
Forwards to webhook → System processes payment
```

---

## Prerequisites

1. ✅ Standard Bank MyUpdates enabled on your account
2. ✅ Email notifications configured in Standard Bank app
3. ✅ Backend running with webhook endpoint active
4. ✅ Webhook API key configured

---

## Option 1: Zapier Integration (Recommended - Easiest)

### Cost
- **Free Plan:** 100 tasks/month (sufficient for small centers)
- **Starter Plan:** $19.99/month (750 tasks)

### Setup Steps

#### Step 1: Create Zapier Account
1. Go to [zapier.com](https://zapier.com)
2. Sign up for free account
3. Verify your email

#### Step 2: Enable Standard Bank MyUpdates
1. Open Standard Bank app
2. Go to **Settings** → **Notifications**
3. Enable **MyUpdates**
4. Select **Email** as notification method
5. Ensure emails go to your Gmail/email account

#### Step 3: Create Gmail Filter (Optional but Recommended)
1. Open Gmail settings
2. Create filter for emails from: `noreply@standardbank.co.za`
3. Add label: `StandardBank-Notifications`
4. Never send to spam

#### Step 4: Create Zapier "Zap"

1. **Click "Create Zap"**

2. **Set Trigger:**
   - App: **Gmail**
   - Event: **New Email**
   - Connect your Gmail account
   - Label: `StandardBank-Notifications` (or leave blank)
   - From: `noreply@standardbank.co.za`
   - Test trigger

3. **Set Action:**
   - App: **Webhooks by Zapier**
   - Event: **POST**
   - URL: `https://your-domain.com/api/webhook/myupdates`
   - Payload Type: **JSON**
   - Data:
     ```json
     {
       "email_id": "{{id}}",
       "received_at": "{{date}}",
       "sender": "{{from}}",
       "subject": "{{subject}}",
       "body": "{{body_plain}}",
       "api_key": "YOUR_WEBHOOK_API_KEY",
       "source": "ZAPIER"
     }
     ```
   - Headers:
     - `Content-Type`: `application/json`
     - `X-API-Key`: `YOUR_WEBHOOK_API_KEY`

4. **Test Action**
   - Send test email from Standard Bank format
   - Check if webhook receives it
   - Verify in backend logs

5. **Turn On Zap**

#### Step 5: Test End-to-End

1. Make a test payment with student number in reference
2. Wait for Standard Bank email (~30-60 seconds)
3. Zapier should capture and forward within seconds
4. Check webhook stats: `http://localhost:8080/api/webhook/myupdates/stats?apiKey=YOUR_KEY`
5. Verify payment appears in dashboard

---

## Option 2: Make.com Integration (Alternative)

### Cost
- **Free Plan:** 1,000 operations/month
- **Core Plan:** $9/month (10,000 operations)

### Setup Steps

#### Step 1: Create Make.com Account
1. Go to [make.com](https://make.com)
2. Sign up for free
3. Verify email

#### Step 2: Create Scenario

1. **Click "Create a new scenario"**

2. **Add Gmail Module:**
   - Search: **Gmail**
   - Select: **Watch Emails**
   - Connect Gmail account
   - Criteria:
     - From: `noreply@standardbank.co.za`
     - Subject contains: `Transaction` or `Payment`
   - Max results: 10
   - Schedule: Every 5 minutes

3. **Add HTTP Module:**
   - Search: **HTTP**
   - Select: **Make a Request**
   - URL: `https://your-domain.com/api/webhook/myupdates`
   - Method: `POST`
   - Headers:
     - `Content-Type`: `application/json`
     - `X-API-Key`: `YOUR_WEBHOOK_API_KEY`
   - Body Type: **Raw**
   - Request Content: **JSON**
   - Body:
     ```json
     {
       "email_id": "{{1.id}}",
       "received_at": "{{1.date}}",
       "sender": "{{1.from}}",
       "subject": "{{1.subject}}",
       "body": "{{1.textPlain}}",
       "api_key": "YOUR_WEBHOOK_API_KEY",
       "source": "MAKE_COM"
     }
     ```

4. **Test Scenario**
   - Run once to test
   - Check webhook receives data

5. **Activate Scenario**
   - Click **Scheduling** toggle to ON
   - Set to run every 5 minutes

---

## Option 3: Gmail Apps Script (Free - Technical)

### Cost
**Free** - No monthly fees

### Setup Steps

#### Step 1: Enable MyUpdates
Same as Option 1

#### Step 2: Create Gmail Filter
Same as Option 1

#### Step 3: Create Apps Script

1. **Open Google Apps Script**
   - Go to [script.google.com](https://script.google.com)
   - Click **New Project**
   - Name: `StandardBankWebhookForwarder`

2. **Paste Code:**
```javascript
// Configuration
const WEBHOOK_URL = 'https://your-domain.com/api/webhook/myupdates';
const API_KEY = 'YOUR_WEBHOOK_API_KEY';
const LABEL_NAME = 'StandardBank-Notifications';
const PROCESSED_LABEL = 'Processed';

function forwardStandardBankEmails() {
  const label = GmailApp.getUserLabelByName(LABEL_NAME);
  const processedLabel = GmailApp.getUserLabelByName(PROCESSED_LABEL) ||
                         GmailApp.createLabel(PROCESSED_LABEL);

  if (!label) {
    Logger.log('Label not found: ' + LABEL_NAME);
    return;
  }

  // Get unprocessed emails
  const threads = label.getThreads(0, 50);

  threads.forEach(thread => {
    const messages = thread.getMessages();

    messages.forEach(message => {
      // Skip if already processed
      if (thread.getLabels().some(l => l.getName() === PROCESSED_LABEL)) {
        return;
      }

      const sender = message.getFrom();

      // Only process Standard Bank emails
      if (!sender.includes('standardbank.co.za')) {
        return;
      }

      // Prepare payload
      const payload = {
        email_id: message.getId(),
        received_at: message.getDate().toISOString(),
        sender: sender,
        subject: message.getSubject(),
        body: message.getPlainBody(),
        api_key: API_KEY,
        source: 'GMAIL_SCRIPT'
      };

      // Send to webhook
      try {
        const options = {
          method: 'post',
          contentType: 'application/json',
          headers: {
            'X-API-Key': API_KEY
          },
          payload: JSON.stringify(payload),
          muteHttpExceptions: true
        };

        const response = UrlFetchApp.fetch(WEBHOOK_URL, options);
        const statusCode = response.getResponseCode();

        if (statusCode === 200) {
          Logger.log('Successfully forwarded email: ' + message.getSubject());
          thread.addLabel(processedLabel);
        } else {
          Logger.log('Error: ' + statusCode + ' - ' + response.getContentText());
        }

      } catch (error) {
        Logger.log('Error forwarding email: ' + error);
      }
    });
  });
}
```

3. **Configure Project:**
   - Replace `YOUR_WEBHOOK_API_KEY` with actual key
   - Replace `your-domain.com` with your server URL
   - Save project (Ctrl+S / Cmd+S)

4. **Set Up Trigger:**
   - Click **Triggers** (clock icon)
   - Click **Add Trigger**
   - Function: `forwardStandardBankEmails`
   - Event source: **Time-driven**
   - Type: **Minutes timer**
   - Interval: **Every 5 minutes**
   - Save

5. **Authorize:**
   - Run function manually once
   - Authorize Gmail access
   - Check logs

---

## Testing Your Setup

### Test Webhook Endpoint

```bash
curl -X POST "http://localhost:8080/api/webhook/myupdates" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YOUR_WEBHOOK_API_KEY" \
  -d '{
    "email_id": "test-001",
    "sender": "noreply@standardbank.co.za",
    "subject": "Transaction Notification",
    "body": "You have received a payment\nDate: 15/01/2025\nAmount: R 1,500.00\nReference: STU-2025-001 January Fee\nBalance: R 45,230.50"
  }'
```

**Expected Response:**
```json
{
  "status": "accepted",
  "message": "Notification queued for processing",
  "email_id": "test-001"
}
```

### Check Webhook Stats

```bash
curl "http://localhost:8080/api/webhook/myupdates/stats?apiKey=YOUR_WEBHOOK_API_KEY"
```

### View Backend Logs

```bash
# Check application logs for processing
tail -f backend/logs/application.log
```

---

## Troubleshooting

### Webhook Not Receiving Emails

**Check:**
1. ✅ Backend is running: `curl http://localhost:8080/api/webhook/health`
2. ✅ API key is correct in forwarding service
3. ✅ Gmail filter is working (emails have correct label)
4. ✅ Forwarding service is active (Zapier Zap ON, Make scenario ON, Apps Script trigger enabled)
5. ✅ Check service logs for errors

### Emails Not Parsing Correctly

**Check:**
1. ✅ Email body format matches Standard Bank MyUpdates
2. ✅ Backend logs show parsing errors
3. ✅ Test with sample email body

**Debug:**
```bash
# Check recent failed notifications
curl "http://localhost:8080/api/webhook/myupdates/stats?apiKey=YOUR_KEY" | jq '.failed_count'
```

### Payments Not Matching to Students

**Check:**
1. ✅ Student number in payment reference (e.g., `STU-2025-001`)
2. ✅ Student exists in database with that number
3. ✅ Check unmatched transactions: `http://localhost:5173/transactions`
4. ✅ Manually assign if needed

---

## Security Best Practices

### 1. Protect API Key
- Never commit API key to version control
- Use environment variable: `WEBHOOK_API_KEY=xxx`
- Rotate key every 3-6 months

### 2. Use HTTPS in Production
- Get SSL certificate (Let's Encrypt free)
- Update webhook URL to `https://`

### 3. Monitor Webhook Activity
- Check stats daily
- Review failed notifications weekly
- Alert on unusual activity

### 4. Validate Email Sender
- Backend already checks sender domain
- Reject emails not from standardbank.co.za

---

## Cost Comparison

| Service | Free Tier | Paid Plan | Best For |
|---------|-----------|-----------|----------|
| **Zapier** | 100 tasks/month | $19.99/month (750) | Ease of use |
| **Make.com** | 1,000 ops/month | $9/month (10K) | More operations |
| **Gmail Apps Script** | Unlimited | Free | Technical users |

---

## Recommended Setup

**For Small Centers (< 100 students):**
→ Use **Zapier Free Plan** or **Gmail Apps Script**

**For Medium Centers (100-300 students):**
→ Use **Make.com Core Plan** ($9/month)

**For Large Centers (> 300 students):**
→ Use **Gmail Apps Script** (free, unlimited)

---

## Next Steps

1. Choose integration method
2. Set up email forwarding
3. Test with sample transaction
4. Make real payment with student number
5. Verify automatic matching works
6. Train staff on reviewing unmatched transactions

---

## Support

If you encounter issues:
1. Check backend logs
2. Review webhook stats endpoint
3. Test webhook with curl
4. Check forwarding service logs
5. Contact support with error details

---

## Monitoring Checklist

Weekly:
- [ ] Check webhook success rate
- [ ] Review unmatched transactions
- [ ] Verify forwarding service is active

Monthly:
- [ ] Review API key security
- [ ] Check service costs vs usage
- [ ] Test end-to-end flow with sample payment

---

This completes your webhook integration setup! The system will now automatically process payments as they come in from Standard Bank.
