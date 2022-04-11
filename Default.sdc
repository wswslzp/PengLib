
###########################################
##      Clock domain clk
###########################################
create_clock -period 0.05 -name clk [get_ports clk]

set_clock_latency 1.0 -early -source [get_clocks clk]
set_clock_latency 1.0 -late -source [get_clocks clk]


set_clock_latency -rise 1.0 -min [get_clocks clk]
set_clock_latency -fall 1.0 -min [get_clocks clk]


set_clock_latency -rise 1.0 -max [get_clocks clk]
set_clock_latency -fall 1.0 -max [get_clocks clk]

set_clock_uncertainty [get_clocks clk] -rise -setup 1.0

set_clock_transition 1.0 -rise -max [get_clocks clk]


###########################################
##      Clock domain ext0_clk
###########################################
create_clock -period 0.3 -name ext0_clk [get_ports ext0_clk]

set_clock_latency 1.0 -early -source [get_clocks ext0_clk]
set_clock_latency 1.0 -late -source [get_clocks ext0_clk]


set_clock_latency -rise 1.0 -min [get_clocks ext0_clk]
set_clock_latency -fall 1.0 -min [get_clocks ext0_clk]


set_clock_latency -rise 1.0 -max [get_clocks ext0_clk]
set_clock_latency -fall 1.0 -max [get_clocks ext0_clk]

set_clock_uncertainty [get_clocks ext0_clk] -rise -setup 1.0

set_clock_transition 1.0 -rise -max [get_clocks ext0_clk]


###########################################
##      Sub-clock domain c10
###########################################
create_generated_clock -name c10 -source [get_clocks ext0_clk] -divide_by 2 [get_pins toplevel/clockDivider_3/c10]

set_clock_latency 1.0 -early -source [get_clocks c10]
set_clock_latency 1.0 -late -source [get_clocks c10]


set_clock_latency -rise 1.0 -min [get_clocks c10]
set_clock_latency -fall 1.0 -min [get_clocks c10]


set_clock_latency -rise 1.0 -max [get_clocks c10]
set_clock_latency -fall 1.0 -max [get_clocks c10]

set_clock_uncertainty [get_clocks c10] -rise -setup 1.0

set_clock_transition 1.0 -rise -max [get_clocks c10]


###########################################
##      Sub-clock domain c11
###########################################
create_generated_clock -name c11 -source [get_clocks ext0_clk] -divide_by 4 [get_pins toplevel/clockDivider_4/c11]

set_clock_latency 1.0 -early -source [get_clocks c11]
set_clock_latency 1.0 -late -source [get_clocks c11]


set_clock_latency -rise 1.0 -min [get_clocks c11]
set_clock_latency -fall 1.0 -min [get_clocks c11]


set_clock_latency -rise 1.0 -max [get_clocks c11]
set_clock_latency -fall 1.0 -max [get_clocks c11]

set_clock_uncertainty [get_clocks c11] -rise -setup 1.0

set_clock_transition 1.0 -rise -max [get_clocks c11]


###########################################
##      Sub-clock domain c2
###########################################
create_generated_clock -name c2 -source [get_clocks clk] -divide_by 2 [get_pins toplevel/clockDivider_5/c2]

set_clock_latency 1.0 -early -source [get_clocks c2]
set_clock_latency 1.0 -late -source [get_clocks c2]


set_clock_latency -rise 1.0 -min [get_clocks c2]
set_clock_latency -fall 1.0 -min [get_clocks c2]


set_clock_latency -rise 1.0 -max [get_clocks c2]
set_clock_latency -fall 1.0 -max [get_clocks c2]

set_clock_uncertainty [get_clocks c2] -rise -setup 1.0

set_clock_transition 1.0 -rise -max [get_clocks c2]

