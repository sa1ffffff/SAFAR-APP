# SAFAR Help & Support Screen - Visual Preview

```
┌─────────────────────────────────────────┐
│  ←  Help & Support                      │  ← TopAppBar (dark bg)
├─────────────────────────────────────────┤
│                                         │
│              🔵 Help Icon               │  ← Header
│      Frequently Asked Questions         │
│  Find answers to common questions...    │
│                                         │
├─────────────────────────────────────────┤
│                                         │
│  🚀 Getting Started                     │  ← Category Header (Green)
│  ┌───────────────────────────────────┐ │
│  │ What is SAFAR?                 ▼  │ │  ← FAQ Card (collapsed)
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ How does SAFAR work?           ▲  │ │  ← FAQ Card (expanded)
│  │                                   │ │
│  │ SAFAR has two user types:         │ │
│  │ • Drivers: Post available seats   │ │
│  │ • Passengers: Browse rides...     │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ Is SAFAR free to use?          ▼  │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ➕ Posting Rides                       │  ← Category Header
│  ┌───────────────────────────────────┐ │
│  │ How do I post a ride?          ▼  │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ Can I edit a ride after...     ▼  │ │
│  └───────────────────────────────────┘ │
│                                         │
│  🔍 Finding Rides                       │
│  💬 Chat & Communication                │
│  🛡️ Safety & Trust                     │
│  💰 Payments                            │
│  👤 Account & Profile                   │
│  🔧 Troubleshooting                     │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │        🎧 Headset Icon            │ │  ← Contact Card
│  │     Still need help?              │ │
│  │  Contact our support team         │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐ │ │
│  │  │ 📧 support@safar.app        │ │ │  ← Button (Yellow)
│  │  └─────────────────────────────┘ │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

## Color Scheme

- **Background**: #0A0A0A (Very dark black)
- **Cards**: #111111 (Dark gray surface)
- **Text Primary**: #F0F0F0 (White)
- **Text Muted**: #6B6B6B (Gray)
- **Category Icons**: #22C55E (Green)
- **Yellow Accent**: #FFD600 (Support button)

## Interaction Flow

### Step 1: User Opens Help
```
Profile Screen → Tap "Help & Support" → HelpActivity launches
```

### Step 2: Browse FAQs
```
Scroll through categories → See questions at a glance
```

### Step 3: Read Answer
```
Tap question → Card expands with smooth animation → See full answer
Tap again → Card collapses
```

### Step 4: Contact Support (if needed)
```
Scroll to bottom → Tap email button → Email app opens
```

## Animations

### Card Expansion
- **Duration**: 300ms
- **Easing**: FastOutSlowInEasing (Material standard)
- **Effect**: Smooth vertical expand/collapse
- **Chevron**: Rotates 180° when expanded

### Visual Feedback
- **Tap**: Material ripple effect on cards
- **Scroll**: Smooth scroll with momentum
- **Transitions**: Fade-in for content

## Accessibility Features

✅ High contrast text (white on dark)  
✅ Touch targets: 48dp minimum  
✅ Clear visual hierarchy  
✅ Icon + text labels  
✅ No reliance on color alone  
✅ Screen reader compatible  

## Mobile Responsiveness

- **Small phones**: 320dp+ supported
- **Medium phones**: Optimal experience
- **Large phones/tablets**: More content visible
- **Landscape**: Horizontal scrolling supported

## Content Strategy

### Answer Format
Each answer follows this structure:

1. **Direct Answer**: First sentence answers the question
2. **Details**: Bullet points or numbered steps
3. **Context**: Additional helpful information
4. **Related Info**: Cross-references when needed

### Writing Style
- **Conversational**: Friendly, approachable tone
- **Concise**: Short sentences, clear language
- **Action-Oriented**: Tell users what to do
- **Localized**: Pakistan-specific (Rupees, JazzCash, etc.)

---

**Status**: Ready to launch! 🚀
**Total Implementation Time**: Complete
**Files Modified/Created**: 7 files
**Lines of Code**: ~700 lines
