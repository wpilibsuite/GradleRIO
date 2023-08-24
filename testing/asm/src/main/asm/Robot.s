.set STACK_SIZE, 48
.set PWM_LOCATION, 44
.set DIO_LOCATION, 40
.set EVENT_LOCATION, 36
.set COUNTER_LOCATION, 32

.global main

.data

.LC0:
    .ascii "Failed to initialize the HAL\000"

.LC1:
    .ascii "Initialized the HAL\000"

.LC2:
    .ascii "Looping Disabled\000"

.LC3:
    .ascii "Looping Teleop\000"

.LC4:
    .ascii "Looping Test\000"

.LC5:
    .ascii "Looping Auton\000"

.text

printCurrentState:
    ldr     r3, [r0]
    add     r3, r3, #1
    cmp     r3, #50
    str     r3, [r0]
    bxne    lr

    mov     r3, #0
    str     r3, [r0]

    cmp     r1, #0
    beq     .printCurrentStateDisabled
    cmp     r1, #1
    beq     .printCurrentStateTeleop
    cmp     r1, #2
    beq     .printCurrentStateTest

    movw    r0, #:lower16:.LC5
    movt    r0, #:upper16:.LC5
    b       puts

.printCurrentStateDisabled:
    movw    r0, #:lower16:.LC2
    movt    r0, #:upper16:.LC2
    b       puts

.printCurrentStateTeleop:
    movw    r0, #:lower16:.LC3
    movt    r0, #:upper16:.LC3
    b       puts

.printCurrentStateTest:
    movw    r0, #:lower16:.LC4
    movt    r0, #:upper16:.LC4
    b       puts

getDSMode:
        push    {r4, lr}
        sub     sp, sp, #8
        add     r0, sp, #4
        bl      HAL_GetControlWord
        ldrb    r3, [sp, #4]    @ zero_extendqisi2
        ands    r4, r3, #1
        beq     .getDSModeDisabled
        tst     r3, #2
        bne     .getDSModeAutonomous
        tst     r3, #4
        bne     .getDSModeTest
        bl      HAL_ObserveUserProgramTeleop
        mov     r0, #1
.getDSModeEnd:
        add     sp, sp, #8
        pop     {r4, pc}
.getDSModeTest:
        bl      HAL_ObserveUserProgramTest
        mov     r0, #2
        add     sp, sp, #8
        pop     {r4, pc}
.getDSModeDisabled:
        bl      HAL_ObserveUserProgramDisabled
        mov     r0, r4
        add     sp, sp, #8
        pop     {r4, pc}
.getDSModeAutonomous:
        bl      HAL_ObserveUserProgramAutonomous
        mov     r0, #3
        b       .getDSModeEnd



main:
    push    {r4, lr}

    sub     sp, sp, #STACK_SIZE
    mov     r0, #0
    str     r0, [sp, #COUNTER_LOCATION]

    mov     r1, #0
    mov     r0, #500
    bl      HAL_Initialize
    cmp     r0, #0
    beq     .FAIL_HAL_INIT
    bl      HAL_ObserveUserProgramStarting

    movw    r0, #:lower16:.LC1
    movt    r0, #:upper16:.LC1
    bl      puts

    mov     r0, #2
    bl      HAL_GetPort // Result in r0
    mov     r1, #0
    str     r1, [sp, #4]
    add     r2, sp, #4
    bl      HAL_InitializePWMPort // (HAL_GetPort, NULL, &status)
    str     r0, [sp, #PWM_LOCATION]
    ldr     r0, [sp, #4]
    cmp     r0, #0
    bne     .FAIL_GET_LAST_ERROR

    ldr     r0, [sp, #PWM_LOCATION]
    mov     r1, #2000
    mov     r2, #1501
    mov     r3, #1500
    mov     r4, #1499
    str     r4, [sp]
    mov     r4, #1000
    str     r4, [sp, #4]
    add     r4, sp, #12
    str     r4, [sp, #8]
    mov     r4, #0
    str     r4, [sp, #12]
    bl      HAL_SetPWMConfigMicroseconds

    mov     r0, #2
    bl      HAL_GetPort
    mov     r1, #1
    mov     r2, #0
    str     r2, [sp, #4]
    add     r3, sp, #4
    bl      HAL_InitializeDIOPort
    str     r0, [sp, #DIO_LOCATION]
    ldr     r0, [sp, #4]
    cmp     r0, #0
    bne     .FAIL_GET_LAST_ERROR_CLEANUP_PWM

    mov     r0, #0
    mov     r1, #0
    bl      WPI_CreateEvent
    str     r0, [sp, #EVENT_LOCATION]

    bl      HAL_ProvideNewDataEventHandle

    // Main loop
.LOOP:

    ldr     r0, [sp, #EVENT_LOCATION]
    // r1 is unused
    mov     r2, #0 // r2 and r3 are the double
    mov     r3, #0
    movt    r3, 16368
    add     r4, sp, #12
    str     r4, [sp] // First stack variable is timed out
    mov     r4, #0
    str     r4, [sp, #12]
    bl      WPI_WaitForObjectTimeout
    cmp     r0, #0
    beq     .LOOP

    bl      HAL_RefreshDSData

    bl      getDSMode
    mov     r1, r0
    mov     r4, r0
    add     r0, sp, #COUNTER_LOCATION
    bl      printCurrentState

    cmp     r4, #1
    bne     .LOOP

    ldr     r0, [sp, #DIO_LOCATION]
    mov     r4, #0
    str     r4, [sp, #4]
    add     r1, sp, #4
    bl      HAL_GetDIO

    cmp     r0, #0
    beq     .STOP_MOTOR

    mov     r2, #0 // r2 and r3 are the double
    mov     r3, #0
    movt    r3, #16368


    b .DO_MOTOR
.STOP_MOTOR:

    mov     r2, #0 // r2 and r3 are the double
    mov     r3, #0

.DO_MOTOR:
    ldr     r0, [sp, #DIO_LOCATION]
    add     r4, sp, #12
    str     r4, [sp] // First stack variable is timed out
    mov     r4, #0
    str     r4, [sp, #12]
    bl HAL_SetPWMSpeed

    b .LOOP

    ldr     r0, [sp, #EVENT_LOCATION]
    bl      HAL_RemoveNewDataEventHandle

    ldr     r0, [sp, #EVENT_LOCATION]
    bl      WPI_DestroyEvent

    ldr     r0, [sp, #DIO_LOCATION]
    bl      HAL_FreeDIOPort

    ldr     r0, [sp, #PWM_LOCATION]
    mov     r1, #0
    str     r1, [sp, #4]
    add     r1, sp, #4

    bl      HAL_FreePWMPort

    mov     r0, #0

.END_MAIN:
    add     sp, sp, #STACK_SIZE
    pop     {r4, pc}


.FAIL_HAL_INIT:
    movw    r0, #:lower16:.LC0
    movt    r0, #:upper16:.LC0
    bl      puts
    mov     r0, #1
    b       .END_MAIN


.FAIL_GET_LAST_ERROR:
    str     r0, [sp, #4]
    add     r0, sp, #4
    bl      HAL_GetLastError
    bl      puts
    mov     r0, #1
    b       .END_MAIN

.FAIL_GET_LAST_ERROR_CLEANUP_PWM:
    str     r0, [sp, #4]
    add     r0, sp, #4
    bl      HAL_GetLastError
    bl      puts

    ldr     r0, [sp, #PWM_LOCATION]
    str     r1, [sp, #4]
    add     r1, sp, #4
    bl      HAL_FreePWMPort

    mov     r0, #1
    b       .END_MAIN
