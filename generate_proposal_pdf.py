from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.lib.enums import TA_JUSTIFY, TA_CENTER, TA_LEFT
from reportlab.lib.colors import HexColor, black, white
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

# Create PDF
pdf_file = "SAFAR_Project_Proposal.pdf"
doc = SimpleDocTemplate(pdf_file, pagesize=A4, topMargin=0.75*inch, bottomMargin=0.75*inch)

# Container for the 'Flowable' objects
elements = []

# Define styles
styles = getSampleStyleSheet()

# Custom styles
title_style = ParagraphStyle(
    'CustomTitle',
    parent=styles['Heading1'],
    fontSize=28,
    textColor=HexColor('#0A0A0A'),
    spaceAfter=10,
    alignment=TA_CENTER,
    fontName='Helvetica-Bold'
)

subtitle_style = ParagraphStyle(
    'CustomSubtitle',
    parent=styles['Normal'],
    fontSize=14,
    textColor=HexColor('#666666'),
    spaceAfter=20,
    alignment=TA_CENTER,
    fontStyle='italic'
)

heading_style = ParagraphStyle(
    'CustomHeading',
    parent=styles['Heading2'],
    fontSize=16,
    textColor=HexColor('#0A0A0A'),
    spaceAfter=12,
    spaceBefore=20,
    fontName='Helvetica-Bold',
    borderColor=HexColor('#FFD600'),
    borderWidth=0,
    borderPadding=5,
    leftIndent=15,
    borderRadius=0
)

body_style = ParagraphStyle(
    'CustomBody',
    parent=styles['Normal'],
    fontSize=11,
    textColor=black,
    spaceAfter=10,
    alignment=TA_JUSTIFY,
    leading=14
)

bullet_style = ParagraphStyle(
    'CustomBullet',
    parent=styles['Normal'],
    fontSize=11,
    textColor=black,
    spaceAfter=8,
    leftIndent=20,
    bulletIndent=10,
    leading=14
)

highlight_style = ParagraphStyle(
    'Highlight',
    parent=styles['Normal'],
    fontSize=11,
    textColor=black,
    spaceAfter=12,
    spaceBefore=12,
    leftIndent=20,
    rightIndent=20,
    backColor=HexColor('#FFF9E6'),
    borderColor=HexColor('#FFD600'),
    borderWidth=1,
    borderPadding=10
)

# Title and header
elements.append(Paragraph("🚗 SAFAR", title_style))
elements.append(Paragraph("سفر", ParagraphStyle('urdu', parent=subtitle_style, fontSize=18, textColor=HexColor('#FFD600'))))
elements.append(Paragraph("Community Carpool Marketplace", subtitle_style))
elements.append(Paragraph("Android Application - Project Proposal", subtitle_style))
elements.append(Spacer(1, 0.3*inch))

# 1. Problem Statement
elements.append(Paragraph("1. Problem Statement", heading_style))
elements.append(Paragraph(
    "Transportation in Pakistan, particularly for intercity travel, faces significant challenges that affect millions of commuters daily:",
    body_style
))

problems = [
    "<b>High Transportation Costs:</b> Individual car ownership and taxi services are expensive for long-distance travel. Public transport, while affordable, often lacks comfort and reliability.",
    "<b>Empty Seats, Wasted Resources:</b> Thousands of private vehicles travel between major cities (Lahore, Islamabad, Karachi, etc.) with multiple empty seats. This represents wasted fuel, increased carbon emissions, and missed opportunities for cost sharing.",
    "<b>Lack of Trusted Ride-Sharing Platforms:</b> While international ride-sharing platforms exist, they are not optimized for Pakistan's unique intercity carpooling needs.",
    "<b>Safety and Trust Concerns:</b> Informal carpooling arrangements lack proper verification, rating systems, and safety mechanisms.",
    "<b>Communication Barriers:</b> Coordination is fragmented across multiple platforms with no centralized communication or booking system."
]

for problem in problems:
    elements.append(Paragraph(f"• {problem}", bullet_style))

elements.append(Spacer(1, 0.1*inch))
elements.append(Paragraph(
    "<b>Core Problem:</b> There is no simple, safe, and community-driven platform in Pakistan that allows drivers to monetize empty seats while providing affordable, flexible transportation options to passengers traveling the same route.",
    highlight_style
))

# 2. Proposed Solution
elements.append(Paragraph("2. Proposed Solution", heading_style))
elements.append(Paragraph(
    "<b>SAFAR</b> (سفر — \"Journey\" in Urdu) is a peer-to-peer ride-sharing Android application designed specifically for Pakistan's intercity travel market. It connects drivers with available seats to passengers heading in the same direction, creating a community-driven carpool marketplace.",
    body_style
))

elements.append(Paragraph("<b>Key Features:</b>", body_style))

features = [
    "<b>🔐 Secure Authentication:</b> Firebase Phone OTP authentication ensures verified users",
    "<b>🚗 Post Ride Offers:</b> Drivers create ride ads with origin, destination, time, seats, and price",
    "<b>🗺️ Map-Based Discovery:</b> Interactive Google Maps interface with custom markers",
    "<b>📩 Request System:</b> Passengers send join requests; drivers accept/reject",
    "<b>💬 Real-Time Chat:</b> Firebase-powered in-app messaging after acceptance",
    "<b>🔔 Push Notifications:</b> Firebase Cloud Messaging for updates",
    "<b>⭐ Rating & Reporting:</b> Community safety through reviews and reports"
]

for feature in features:
    elements.append(Paragraph(f"• {feature}", bullet_style))

elements.append(PageBreak())

# 3. Technology Stack
elements.append(Paragraph("3. Technology Stack", heading_style))

# Development Environment Table
elements.append(Paragraph("<b>3.1 Development Environment</b>", body_style))
dev_env_data = [
    ['Component', 'Specification'],
    ['<b>IDE</b>', 'Android Studio (Latest Stable Version)'],
    ['<b>Primary Language</b>', 'Java'],
    ['<b>Secondary Language</b>', 'Kotlin (for UI components)'],
    ['<b>Build System</b>', 'Gradle 8.7.0'],
    ['<b>Minimum SDK</b>', 'API 24 (Android 7.0)'],
    ['<b>Target SDK</b>', 'API 35 (Android 15)'],
    ['<b>Java Version</b>', 'Java 11'],
    ['<b>Kotlin Version</b>', '2.0.21']
]

dev_table = Table(dev_env_data, colWidths=[2*inch, 4*inch])
dev_table.setStyle(TableStyle([
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#0A0A0A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFD600')),
    ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
    ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
    ('FONTSIZE', (0, 0), (-1, 0), 11),
    ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
    ('BACKGROUND', (0, 1), (-1, -1), HexColor('#f8f9fa')),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#dddddd')),
    ('FONTSIZE', (0, 1), (-1, -1), 10),
    ('TOPPADDING', (0, 1), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 1), (-1, -1), 8),
]))
elements.append(dev_table)
elements.append(Spacer(1, 0.2*inch))

# Core Technologies
elements.append(Paragraph("<b>3.2 Core Technologies</b>", body_style))

tech_list = [
    "<b>Backend (Firebase):</b> Auth (Phone OTP), Firestore (data), Realtime Database (chat), FCM (notifications), Storage (images)",
    "<b>UI Framework:</b> Material Design 3, Jetpack Compose, XML layouts",
    "<b>Maps:</b> Google Maps SDK, Maps Compose, Location Services",
    "<b>Image Loading:</b> Glide 4.16.0, Coil Compose 2.5.0",
    "<b>Architecture:</b> MVP (Model-View-Presenter) pattern"
]

for tech in tech_list:
    elements.append(Paragraph(f"• {tech}", bullet_style))

# 4. Hardware & Software Requirements
elements.append(Paragraph("4. Tools & Requirements", heading_style))

elements.append(Paragraph("<b>4.1 Hardware Requirements</b>", body_style))

hardware_data = [
    ['Item', 'Purpose', 'Specification'],
    ['<b>Development Computer</b>', 'Running Android Studio', '• 8GB RAM (16GB recommended)\n• Intel i5/AMD Ryzen 5+\n• 20GB free space (SSD)\n• Windows/macOS/Linux'],
    ['<b>Android Debug Phone</b>', 'Real device testing', '• Android 7.0+\n• GPS enabled\n• Camera\n• USB debugging enabled']
]

hardware_table = Table(hardware_data, colWidths=[1.5*inch, 1.8*inch, 2.7*inch])
hardware_table.setStyle(TableStyle([
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#0A0A0A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFD600')),
    ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
    ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
    ('FONTSIZE', (0, 0), (-1, 0), 10),
    ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
    ('BACKGROUND', (0, 1), (-1, -1), HexColor('#f8f9fa')),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#dddddd')),
    ('FONTSIZE', (0, 1), (-1, -1), 9),
    ('TOPPADDING', (0, 1), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 1), (-1, -1), 8),
]))
elements.append(hardware_table)
elements.append(Spacer(1, 0.15*inch))

elements.append(Paragraph("<b>Why a Physical Debugging Phone is Essential:</b>", body_style))
debug_reasons = [
    "<b>GPS Testing:</b> Emulators cannot simulate real-world GPS behavior",
    "<b>Firebase Phone Auth:</b> OTP verification requires actual SMS reception",
    "<b>Push Notifications:</b> FCM testing needs real device behavior",
    "<b>Camera Integration:</b> Profile picture uploads require functioning camera",
    "<b>Performance Testing:</b> Real-world performance and battery usage"
]

for reason in debug_reasons:
    elements.append(Paragraph(f"• {reason}", bullet_style))

elements.append(Spacer(1, 0.15*inch))
elements.append(Paragraph("<b>4.2 Software Requirements</b>", body_style))
software_list = [
    "Android Studio (Latest stable)",
    "JDK 11 (Java Development Kit)",
    "Git (Version control)",
    "Firebase Account (Free tier)",
    "Google Cloud Platform account (Maps API key)"
]

for software in software_list:
    elements.append(Paragraph(f"• {software}", bullet_style))

elements.append(PageBreak())

# 5. Architecture
elements.append(Paragraph("5. Architecture & Design", heading_style))
elements.append(Paragraph(
    "<b>MVP (Model-View-Presenter) Pattern:</b> SAFAR follows strict architectural separation for maintainable, testable code:",
    body_style
))

arch_components = [
    "<b>Models:</b> Plain Java POJOs (User, Ride, Message, JoinRequest, Report)",
    "<b>Views:</b> Activities/Fragments handling only UI updates",
    "<b>Presenters:</b> Business logic layer connecting Models and Views",
    "<b>Repositories:</b> Data access layer managing all Firebase operations"
]

for component in arch_components:
    elements.append(Paragraph(f"• {component}", bullet_style))

elements.append(Spacer(1, 0.15*inch))
elements.append(Paragraph("<b>Language Distribution:</b>", body_style))
lang_notes = [
    "<b>Java:</b> All business logic (Models, Repositories, Presenters, Services)",
    "<b>Kotlin:</b> Some UI components (Activities, Fragments, Adapters)",
    "<b>XML:</b> Layout files, drawables, resources"
]

for note in lang_notes:
    elements.append(Paragraph(f"• {note}", bullet_style))

elements.append(Spacer(1, 0.15*inch))
elements.append(Paragraph("<b>Firebase Schema:</b>", body_style))
elements.append(Paragraph("• <b>Firestore:</b> /users, /rides, /requests, /reports, /ratings", bullet_style))
elements.append(Paragraph("• <b>Realtime Database:</b> /chats/{rideId}/messages (for instant messaging)", bullet_style))

# 6. Project Status
elements.append(Paragraph("6. Implementation Status", heading_style))

status_data = [
    ['Feature', 'Status'],
    ['Phone OTP Authentication', '✅ Completed'],
    ['User Profiles', '✅ Completed'],
    ['Post Ride Advertisements', '✅ Completed'],
    ['Map-Based Ride Discovery', '✅ Completed'],
    ['Join Request System', '✅ Completed'],
    ['Real-Time Chat', '✅ Completed'],
    ['Push Notifications', '✅ Completed'],
    ['Reporting System', '✅ Completed'],
    ['Rating System UI', '🔄 In Progress'],
    ['Testing & Polish', '⏳ Upcoming']
]

status_table = Table(status_data, colWidths=[4*inch, 2*inch])
status_table.setStyle(TableStyle([
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#0A0A0A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFD600')),
    ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
    ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
    ('FONTSIZE', (0, 0), (-1, 0), 11),
    ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
    ('BACKGROUND', (0, 1), (-1, -1), HexColor('#f8f9fa')),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#dddddd')),
    ('FONTSIZE', (0, 1), (-1, -1), 10),
    ('TOPPADDING', (0, 1), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 1), (-1, -1), 8),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#f8f9fa'), white])
]))
elements.append(status_table)

# 7. Expected Outcomes
elements.append(Paragraph("7. Expected Outcomes & Benefits", heading_style))
elements.append(Paragraph("<b>For Users:</b>", body_style))

user_benefits = [
    "Cost savings through shared transportation",
    "Reduced carbon footprint",
    "Community building through shared travel",
    "More flexible than scheduled transport",
    "Safety through verified users and ratings"
]

for benefit in user_benefits:
    elements.append(Paragraph(f"• {benefit}", bullet_style))

elements.append(Spacer(1, 0.1*inch))
elements.append(Paragraph("<b>For Developers/Students:</b>", body_style))

dev_benefits = [
    "Hands-on Firebase ecosystem experience",
    "MVP architecture mastery",
    "Google Maps SDK integration",
    "Java + Kotlin hybrid project experience",
    "Real-time systems and push notifications",
    "Portfolio-worthy full-stack mobile app"
]

for benefit in dev_benefits:
    elements.append(Paragraph(f"• {benefit}", bullet_style))

# 8. Conclusion
elements.append(Paragraph("8. Conclusion", heading_style))
elements.append(Paragraph(
    "SAFAR addresses a real transportation gap in Pakistan by providing a simple, trusted, and community-driven carpool marketplace. Built with industry-standard technologies (<b>Java as primary language, Kotlin for UI enhancements, Android Studio IDE, Firebase backend, Google Maps</b>) and following best practices (MVP architecture, Material Design 3), this project demonstrates comprehensive mobile development skills while solving a practical problem.",
    body_style
))
elements.append(Spacer(1, 0.1*inch))
elements.append(Paragraph(
    "The application requires <b>Android Studio for development</b> and a <b>physical debugging phone</b> for real-world testing of GPS, authentication, and notification features. With core features implemented and safety mechanisms in progress, SAFAR is positioned to become a viable solution for intercity carpooling in Pakistan.",
    body_style
))

# Footer
elements.append(Spacer(1, 0.4*inch))
footer_style = ParagraphStyle('footer', parent=styles['Normal'], fontSize=10, textColor=HexColor('#666666'), alignment=TA_CENTER)
elements.append(Paragraph("───────────────────────────────────────", footer_style))
elements.append(Paragraph("<b>SAFAR — Community Carpool Marketplace</b>", footer_style))
elements.append(Paragraph("سفر • Connecting Journeys, Building Community", footer_style))
elements.append(Paragraph("BSCS Final Year Project • SEECS, NUST • Pakistan", ParagraphStyle('footersmall', parent=footer_style, fontSize=9)))

# Build PDF
doc.build(elements)
print(f"✅ PDF created successfully: {pdf_file}")
